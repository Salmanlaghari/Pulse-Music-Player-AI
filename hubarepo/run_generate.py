"""Runner that injects a urllib-backed requests shim then executes generate.py."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

# Inject requests shim before generate.py is imported
import requests_shim
sys.modules["requests"] = requests_shim

os.chdir(HERE)

# Execute generate.py as __main__
with open(os.path.join(HERE, "generate.py"), encoding="utf-8") as f:
    code = f.read()

exec(compile(code, "generate.py", "exec"), {"__name__": "__main__", "__file__": os.path.join(HERE, "generate.py")})