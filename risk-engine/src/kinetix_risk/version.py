import os

__version__ = "0.1.0"

GIT_COMMIT_SHA = os.environ.get("GIT_COMMIT_SHA", "dev")


def get_model_version() -> str:
    short_sha = GIT_COMMIT_SHA[:8] if len(GIT_COMMIT_SHA) > 8 else GIT_COMMIT_SHA
    return f"{__version__}-{short_sha}"
