import pytest

import config


def test_parse_db_url_extracts_host_port_database():
    result = config.parse_db_url("jdbc:mysql://localhost:3306/bscene")

    assert result == {"host": "localhost", "port": 3306, "database": "bscene"}


def test_parse_db_url_defaults_port_to_3306_when_missing():
    result = config.parse_db_url("jdbc:mysql://db.internal/bscene")

    assert result["port"] == 3306


def test_parse_db_url_works_without_jdbc_prefix():
    result = config.parse_db_url("mysql://localhost:3306/bscene")

    assert result["database"] == "bscene"


def test_parse_db_url_rejects_non_mysql_scheme():
    with pytest.raises(ValueError):
        config.parse_db_url("jdbc:postgresql://localhost:5432/bscene")


def test_parse_db_url_rejects_missing_database_name():
    with pytest.raises(ValueError):
        config.parse_db_url("jdbc:mysql://localhost:3306/")
