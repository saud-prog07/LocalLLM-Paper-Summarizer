from utils.tokenizer import chunk_text


def create_chunks(text: str, chunk_size_tokens: int = 1200) -> list[str]:
    """
    Wrapper around tokenizer.chunk_text for clarity.
    """
    return chunk_text(text, chunk_size_tokens)

