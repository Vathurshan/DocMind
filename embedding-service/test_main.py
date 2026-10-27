from fastapi.testclient import TestClient

import main


class FakeModel:
    def encode(self, text):
        if text == "fail":
            raise RuntimeError("model exploded")
        return FakeVector([0.1] * 384)


class FakeVector(list):
    def tolist(self):
        return list(self)


class WrongDimensionModel:
    def encode(self, text):
        return FakeVector([0.1, 0.2])


def setup_function():
    main.model = FakeModel()


def test_health():
    client = TestClient(main.app)
    response = client.get("/")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "running"
    assert body["dimensions"] == 384


def test_valid_embedding_shape():
    client = TestClient(main.app)
    response = client.post("/embed", json={"text": "What is a security threat?"})
    assert response.status_code == 200
    body = response.json()
    assert body["dimensions"] == 384
    assert len(body["embedding"]) == 384
    assert all(isinstance(value, float) for value in body["embedding"])


def test_blank_text_is_rejected():
    client = TestClient(main.app)
    response = client.post("/embed", json={"text": "   "})
    assert response.status_code == 400


def test_missing_text_is_rejected():
    client = TestClient(main.app)
    response = client.post("/embed", json={"text": ""})
    assert response.status_code == 422 or response.status_code == 400


def test_model_error_returns_500():
    client = TestClient(main.app)
    response = client.post("/embed", json={"text": "fail"})
    assert response.status_code == 500


def test_wrong_dimension_is_rejected():
    main.model = WrongDimensionModel()
    client = TestClient(main.app)
    response = client.post("/embed", json={"text": "hello"})
    assert response.status_code == 500
