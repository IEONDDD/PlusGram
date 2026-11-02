"""Streaming AES-CTR using Android's verified JCA implementation."""
from java import jclass
_JCipher = jclass("javax.crypto.Cipher")
_Key = jclass("javax.crypto.spec.SecretKeySpec")
_IV = jclass("javax.crypto.spec.IvParameterSpec")


class algorithms:
    class AES:
        def __init__(self, key):
            self.key = key


class modes:
    class CTR:
        def __init__(self, iv):
            self.iv = iv


class Cipher:
    def __init__(self, algorithm, mode):
        self.key, self.iv = algorithm.key, mode.iv

    def encryptor(self):
        return _Context(self.key, self.iv)

    decryptor = encryptor


class _Context:
    def __init__(self, key, iv):
        self.cipher = _JCipher.getInstance("AES/CTR/NoPadding")
        self.cipher.init(_JCipher.ENCRYPT_MODE, _Key(key, "AES"), _IV(iv))

    def update(self, data):
        if not data:
            return b""
        result = self.cipher.update(data)
        return bytes(result) if result is not None else b""

    def finalize(self):
        result = self.cipher.doFinal()
        return bytes(result) if result is not None else b""
