def chunk_text(text: str, chunk_size: int = 1200) -> list[str]:
    """
    Split text into chunks of ~chunk_size tokens.
    Here we approximate 1 token ≈ 1 word for simplicity.
    """
    if not text:
        return []

    words = text.split()
    chunks: list[str] = []

    for i in range(0, len(words), chunk_size):
        chunk = " ".join(words[i : i + chunk_size])
        chunks.append(chunk)

    return chunks

