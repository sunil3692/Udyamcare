from classifier import keyword_classify


def test_emergency():
    assert keyword_classify("bhai urgent hai, papa ka accident ho gaya")["category"] == "emergency"
    assert keyword_classify("मैं हॉस्पिटल से बोल रहा हूँ, जल्दी आइए")["category"] == "emergency"


def test_sales():
    assert keyword_classify("sir aapke liye personal loan ka offer hai")["category"] == "sales"


def test_promo():
    assert keyword_classify("congratulations aap lucky draw winner hain")["category"] == "promotional"


def test_unknown():
    assert keyword_classify("hello")["category"] == "unknown"
