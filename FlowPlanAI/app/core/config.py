from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """애플리케이션 설정"""
    
    # App Info
    APP_NAME: str = "FlowPlanAI"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    
    # API Settings
    API_V1_PREFIX: str = "/api/v1"
    
    # Groq AI
    GROQ_API_KEY: str
    GROQ_MODEL: str = "llama-3.3-70b-versatile"  # Groq의 최신 고성능 모델
    
    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
