import os
from pathlib import Path
from urllib.parse import urlparse

from dotenv import load_dotenv

ROOT_DIR = Path(__file__).resolve().parent.parent
ENV_PATH = ROOT_DIR / ".env"

load_dotenv(ENV_PATH)


def parse_db_url(db_url: str) -> dict:
    """jdbc:mysql://host:port/dbname 형태의 문자열에서 host/port/database를 뽑아낸다."""
    raw = db_url[len("jdbc:"):] if db_url.startswith("jdbc:") else db_url
    parsed = urlparse(raw)

    if parsed.scheme != "mysql":
        raise ValueError(f"지원하지 않는 DB_URL 스킴입니다: {raw!r} (mysql://이어야 함)")

    database = parsed.path.lstrip("/")
    if not database:
        raise ValueError(f"DB_URL에 database 이름이 없습니다: {raw!r}")

    return {
        "host": parsed.hostname or "localhost",
        "port": parsed.port or 3306,
        "database": database,
    }


def _require_env(key: str) -> str:
    value = os.environ.get(key)
    if not value:
        raise RuntimeError(
            f"{key} 값이 없습니다. 프로젝트 루트({ROOT_DIR})의 .env를 확인하세요."
        )
    return value


DB_URL = _require_env("DB_URL")
DB_USER = _require_env("DB_USER")
DB_PASSWORD = _require_env("DB_PASSWORD")

_parsed_db_url = parse_db_url(DB_URL)
DB_HOST = _parsed_db_url["host"]
DB_PORT = _parsed_db_url["port"]
DB_NAME = _parsed_db_url["database"]

BAND_TABLE = "band"
BAND_SIMILARITY_TABLE = "band_similarity"
BAND_SIMILARITY_STAGING_TABLE = "band_similarity_new"
BAND_SIMILARITY_OLD_TABLE = "band_similarity_old"

# 유사도 계산 파라미터
MODEL_NAME = "jhgan/ko-sroberta-multitask"
MIN_DESCRIPTION_LENGTH = 15
TOP_K = 10
SIMILARITY_THRESHOLD = 0.55
