import argparse
import logging
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

import numpy as np
import pymysql
from pymysql.connections import Connection
from sentence_transformers import SentenceTransformer

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import config

logger = logging.getLogger("description_similarity")


@dataclass(frozen=True)
class SimilarityPair:
    band_id: int
    similar_band_id: int
    score: float


def configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        stream=sys.stdout,
    )


def get_connection() -> Connection:
    return pymysql.connect(
        host=config.DB_HOST,
        port=config.DB_PORT,
        user=config.DB_USER,
        password=config.DB_PASSWORD,
        database=config.DB_NAME,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def fetch_bands(conn: Connection) -> list[dict]:
    with conn.cursor() as cursor:
        cursor.execute(
            f"SELECT id AS band_id, name, description FROM {config.BAND_TABLE}"
        )
        return cursor.fetchall()


def split_eligible_and_skipped(
    bands: list[dict],
    min_length: int = config.MIN_DESCRIPTION_LENGTH,
) -> tuple[list[dict], list[dict]]:
    """description이 min_length자 이상인 밴드와 그렇지 않은(스킵되는) 밴드를 나눈다."""
    eligible, skipped = [], []
    for band in bands:
        description = (band.get("description") or "").strip()
        if len(description) >= min_length:
            eligible.append({**band, "description": description})
        else:
            skipped.append(band)
    return eligible, skipped


def load_model(model_name: str = config.MODEL_NAME) -> SentenceTransformer:
     return SentenceTransformer(model_name)


def embed_descriptions(model: SentenceTransformer, descriptions: list[str]) -> np.ndarray:
    return model.encode(descriptions, show_progress_bar=False, convert_to_numpy=True)


def compute_similarity(embeddings: np.ndarray) -> np.ndarray:
    """임베딩 행렬로부터 전체 코사인 유사도 행렬을 계산한다.

    현재는 500개 내외라 numpy로 전체를 한 번에 계산하지만,
    밴드 수가 늘어나면 이 함수만 청크 단위 계산으로 교체 예정
    """
    normalized = embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)
    return normalized @ normalized.T


def extract_top_k_pairs(
    band_ids: list[int],
    similarity_matrix: np.ndarray,
    top_k: int = config.TOP_K,
    threshold: float = config.SIMILARITY_THRESHOLD,
) -> list[SimilarityPair]:
    """밴드마다 자기 자신을 제외하고 유사도 상위 top_k, threshold 이상만 추출한다."""
    pairs: list[SimilarityPair] = []
    n = len(band_ids)
    for i in range(n):
        scores = similarity_matrix[i]
        candidates = [(j, scores[j]) for j in range(n) if j != i and scores[j] >= threshold]
        candidates.sort(key=lambda pair: pair[1], reverse=True)
        for j, score in candidates[:top_k]:
            pairs.append(SimilarityPair(band_ids[i], band_ids[j], round(float(score), 4)))
    return pairs


def create_staging_table(conn: Connection) -> None:
    with conn.cursor() as cursor:
        cursor.execute(f"DROP TABLE IF EXISTS {config.BAND_SIMILARITY_STAGING_TABLE}")
        cursor.execute(
            f"CREATE TABLE {config.BAND_SIMILARITY_STAGING_TABLE} "
            f"LIKE {config.BAND_SIMILARITY_TABLE}"
        )
    conn.commit()


def insert_pairs(
    conn: Connection,
    pairs: list[SimilarityPair],
    table: str = config.BAND_SIMILARITY_STAGING_TABLE,
    batch_size: int = 500,
) -> None:
    if not pairs:
        return
    now = datetime.now()
    rows = [(pair.band_id, pair.similar_band_id, pair.score, now) for pair in pairs]
    sql = (
        f"INSERT INTO {table} (band_id, similar_band_id, score, created_at) "
        f"VALUES (%s, %s, %s, %s)"
    )
    with conn.cursor() as cursor:
        for offset in range(0, len(rows), batch_size):
            cursor.executemany(sql, rows[offset:offset + batch_size])
    conn.commit()


def rename_tables(conn: Connection) -> None:
    """band_similarity <-> band_similarity_new를 원자적으로 교체한다.

    MySQL의 다중 테이블 RENAME TABLE은 단일 원자적 연산이라, 이 문장이 끝나면
    band_similarity는 이번에 계산된 새 데이터를 가리키고 이전 데이터는
    band_similarity_old라는 이름으로 남는다 (아직 drop 전).
    """
    with conn.cursor() as cursor:
        cursor.execute(
            f"RENAME TABLE {config.BAND_SIMILARITY_TABLE} TO {config.BAND_SIMILARITY_OLD_TABLE}, "
            f"{config.BAND_SIMILARITY_STAGING_TABLE} TO {config.BAND_SIMILARITY_TABLE}"
        )
    conn.commit()


def drop_table_if_exists(conn: Connection, table: str) -> None:
    with conn.cursor() as cursor:
        cursor.execute(f"DROP TABLE IF EXISTS {table}")
    conn.commit()


def save_similarities(conn: Connection, pairs: list[SimilarityPair], dry_run: bool = False) -> None:
    """band_similarity_new에 적재 후 RENAME TABLE로 스왑한다.

    스왑(rename_tables) 이전 단계에서 예외가 나면 band_similarity_new만 정리하고
    끝낸다 - 기존 band_similarity는 절대 건드리지 않는다. 스왑 자체가 성공한 뒤
    구 테이블(band_similarity_old) 정리에 실패하는 것은 서비스에 영향이 없으므로
    경고만 남기고 배치를 실패 처리하지 않는다.
    """
    if dry_run:
        logger.info("[dry-run] %d개 쌍을 저장할 예정 (테이블 생성/저장 없이 건너뜀)", len(pairs))
        return

    create_staging_table(conn)

    try:
        insert_pairs(conn, pairs)
    except Exception:
        logger.exception(
            "%s에 적재하는 중 실패. 스테이징 테이블만 정리하고, 기존 %s는 그대로 둡니다.",
            config.BAND_SIMILARITY_STAGING_TABLE,
            config.BAND_SIMILARITY_TABLE,
        )
        drop_table_if_exists(conn, config.BAND_SIMILARITY_STAGING_TABLE)
        raise

    try:
        rename_tables(conn)
    except Exception:
        logger.exception(
            "테이블 스왑(RENAME TABLE) 실패. 스테이징 테이블만 정리하고, 기존 %s는 그대로 둡니다.",
            config.BAND_SIMILARITY_TABLE,
        )
        drop_table_if_exists(conn, config.BAND_SIMILARITY_STAGING_TABLE)
        raise

    try:
        drop_table_if_exists(conn, config.BAND_SIMILARITY_OLD_TABLE)
    except Exception:
        logger.warning(
            "스왑은 성공했지만 %s 정리에 실패했습니다. 수동으로 DROP TABLE 해주세요.",
            config.BAND_SIMILARITY_OLD_TABLE,
            exc_info=True,
        )


def run(dry_run: bool = False) -> dict:
    conn = get_connection()
    try:
        bands = fetch_bands(conn)
        eligible, skipped = split_eligible_and_skipped(bands)

        logger.info(
            "밴드 %d개 조회, 스킵 %d개 (description 15자 미만): %s",
            len(bands), len(skipped),
            ", ".join(band["name"] for band in skipped) or "-",
        )

        if len(eligible) < 2:
            logger.info("유사도를 계산할 밴드가 2개 미만이라 종료합니다.")
            return {"processed": len(eligible), "skipped": len(skipped), "pairs_saved": 0}

        band_ids = [band["band_id"] for band in eligible]
        descriptions = [band["description"] for band in eligible]

        model = load_model(config.MODEL_NAME)
        embeddings = embed_descriptions(model, descriptions)
        similarity_matrix = compute_similarity(embeddings)
        pairs = extract_top_k_pairs(band_ids, similarity_matrix)

        if dry_run:
            logger.info("[dry-run] 계산된 쌍 %d개 (저장 생략): %s", len(pairs), pairs)

        save_similarities(conn, pairs, dry_run=dry_run)

        logger.info(
            "완료: 처리 %d개, 스킵 %d개, 저장된 쌍 %d개, 테이블 스왑 %s",
            len(eligible), len(skipped), len(pairs), "생략(dry-run)" if dry_run else "성공",
        )
        return {"processed": len(eligible), "skipped": len(skipped), "pairs_saved": len(pairs)}
    finally:
        conn.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="밴드 description 기반 유사도 계산 배치")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="DB에 아무것도 만들거나 저장하지 않고 콘솔에 결과만 출력",
    )
    args = parser.parse_args()

    configure_logging()
    try:
        run(dry_run=args.dry_run)
    except Exception:
        logger.exception("배치 실행 중 오류가 발생했습니다.")
        sys.exit(1)


if __name__ == "__main__":
    main()
