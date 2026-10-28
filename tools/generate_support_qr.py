"""Regenerate the bundled support QR offline from the configured public URL.

Optional developer tool: Python 3 with qrcode==8.2 and Pillow.
It does not contact the destination or run as part of the Android build.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

import qrcode

root = Path(__file__).resolve().parents[1]
strings = ET.parse(root / "app/src/main/res/values/support.xml").getroot()
url = strings.find("string[@name='support_url']").text
if url != "https://buymeacoffee.com/vitalisdexter":
    raise ValueError("Review the public support destination before generating a changed QR")
qr = qrcode.QRCode(version=4, error_correction=qrcode.constants.ERROR_CORRECT_Q,
                   box_size=12, border=4)
qr.add_data(url)
qr.make(fit=False)
target = root / "app/src/main/res/drawable-nodpi/support_qr.png"
qr.make_image(fill_color="black", back_color="white").save(target)
print(target.relative_to(root).as_posix())
