from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """애플리케이션 설정"""
    
    # App Info
    APP_NAME: str = "FlowPlanAI"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    
    # API Settings
    API_V1_PREFIX: str = "/api/v1"
    
    # Google Gemini
    GEMINI_API_KEY: str
    GEMINI_MODEL: str = "gemini-2.0-flash-exp"  # 최신 모델 (새로운 SDK 사용)
    
    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
