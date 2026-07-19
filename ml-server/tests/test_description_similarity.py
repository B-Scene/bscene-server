import numpy as np

import description_similarity as ds


def test_split_eligible_and_skipped_filters_by_min_length():
    bands = [
        {"band_id": 1, "name": "A", "description": "충분히 긴 설명입니다 열다섯자 이상"},
        {"band_id": 2, "name": "B", "description": "짧음"},
        {"band_id": 3, "name": "C", "description": None},
        {"band_id": 4, "name": "D", "description": "   "},
    ]

    eligible, skipped = ds.split_eligible_and_skipped(bands, min_length=15)

    assert [b["band_id"] for b in eligible] == [1]
    assert [b["band_id"] for b in skipped] == [2, 3, 4]


def test_split_eligible_and_skipped_strips_whitespace_before_measuring_length():
    bands = [{"band_id": 1, "name": "A", "description": "   짧음   "}]

    eligible, skipped = ds.split_eligible_and_skipped(bands, min_length=15)

    assert eligible == []
    assert [b["band_id"] for b in skipped] == [1]


def test_extract_top_k_pairs_excludes_self_and_applies_threshold():
    band_ids = [10, 20, 30]
    similarity_matrix = np.array([
        [1.0, 0.9, 0.5],
        [0.9, 1.0, 0.6],
        [0.5, 0.6, 1.0],
    ])

    pairs = ds.extract_top_k_pairs(band_ids, similarity_matrix, top_k=10, threshold=0.55)

    assert ds.SimilarityPair(10, 20, 0.9) in pairs
    assert ds.SimilarityPair(20, 10, 0.9) in pairs
    assert ds.SimilarityPair(20, 30, 0.6) in pairs
    assert ds.SimilarityPair(30, 20, 0.6) in pairs
    # 10<->30 유사도(0.5)는 threshold(0.55) 미만이라 어느 방향으로도 포함되지 않는다
    assert ds.SimilarityPair(10, 30, 0.5) not in pairs
    assert ds.SimilarityPair(30, 10, 0.5) not in pairs
    assert all(pair.band_id != pair.similar_band_id for pair in pairs)


def test_extract_top_k_pairs_can_be_asymmetric():
    # band 2 기준 top1은 band 3이지만, band 3 기준 top1은 band 2로 서로 일치한다.
    # band 1 기준 top1은 band 2이지만, band 2 기준 top1에는 band 1이 없다 - 의도된 비대칭.
    band_ids = [1, 2, 3]
    similarity_matrix = np.array([
        [1.0, 0.9, 0.0],
        [0.9, 1.0, 0.95],
        [0.0, 0.95, 1.0],
    ])

    pairs = ds.extract_top_k_pairs(band_ids, similarity_matrix, top_k=1, threshold=0.55)
    pairs_by_band = {pair.band_id: pair.similar_band_id for pair in pairs}

    assert pairs_by_band[1] == 2
    assert pairs_by_band[2] == 3
    assert pairs_by_band[3] == 2


def test_extract_top_k_pairs_respects_top_k_limit():
    band_ids = [1, 2, 3, 4]
    similarity_matrix = np.array([
        [1.0, 0.9, 0.8, 0.7],
        [0.9, 1.0, 0.85, 0.75],
        [0.8, 0.85, 1.0, 0.65],
        [0.7, 0.75, 0.65, 1.0],
    ])

    pairs = ds.extract_top_k_pairs(band_ids, similarity_matrix, top_k=2, threshold=0.0)

    pairs_for_band_1 = [pair for pair in pairs if pair.band_id == 1]
    assert len(pairs_for_band_1) == 2
    assert {pair.similar_band_id for pair in pairs_for_band_1} == {2, 3}


def test_extract_top_k_pairs_rounds_score_to_4_decimal_places():
    band_ids = [1, 2]
    similarity_matrix = np.array([
        [1.0, 0.123456789],
        [0.123456789, 1.0],
    ])

    pairs = ds.extract_top_k_pairs(band_ids, similarity_matrix, top_k=10, threshold=0.0)

    assert pairs[0].score == 0.1235
