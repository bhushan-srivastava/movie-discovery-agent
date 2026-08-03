from headroom import compress

synthetic = "\n".join(
    [
        "Example Movie A | rating: 8.1 | genre: Fictional Adventure | year: 2024",
        "Example Movie B | rating: 7.4 | genre: Fictional Comedy | year: 2023",
        "Example Movie A | rating: 8.1 | genre: Fictional Adventure | year: 2024",
        "Example Movie B | rating: 7.4 | genre: Fictional Comedy | year: 2023",
    ]
    + ["boilerplate: fictional tool response; no real database data"] * 20
)
messages = [{"role": "user", "content": synthetic}]

try:
    result = compress(
        messages,
        model="gpt-4o",
        optimize=True,
        compress_user_messages=True,
        target_ratio=0.5,
        protect_recent=0,
        min_tokens_to_compress=20,
    )
    output_messages = result.messages
    output_text = "\n".join(str(message.get("content", "")) for message in output_messages)
    print("status=success")
    print(f"input_chars={len(synthetic)}")
    print(f"output_chars={len(output_text)}")
    print(f"compression_percent={(1 - len(output_text) / len(synthetic)) * 100:.2f}")
    print(f"contains_example_movie_a={'Example Movie A' in output_text}")
    print(f"contains_example_movie_b={'Example Movie B' in output_text}")
    print(f"result_type={type(result).__name__}")
    print(f"tokens_before={result.tokens_before}")
    print(f"tokens_after={result.tokens_after}")
    print(f"tokens_saved={result.tokens_saved}")
    print(f"compression_ratio={result.compression_ratio}")
    print(f"result_fields={sorted(name for name in dir(result) if not name.startswith('_'))}")
except Exception as exc:
    print("status=blocked")
    print(f"exception_type={type(exc).__name__}")
    print(f"exception_summary={str(exc).replace(chr(10), ' ')[:300]}")
    print("synthetic_input_only=true")


