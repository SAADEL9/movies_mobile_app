from pathlib import Path
root = Path(r"C:/Users/user/Desktop/movies_mobile_app")
files = {}
files[root/'app/src/main/res/anim/fade_in.xml'] = '''<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="280"
    android:fromAlpha="0.0"
    android:toAlpha="1.0" />
'''
files[root/'app/src/main/res/anim/slide_in_right.xml'] = '''<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="260"
    android:fromXDelta="100%"
    android:toXDelta="0%" />
'''
files[root/'app/src/main/res/anim/slide_out_left.xml'] = '''<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="260"
    android:fromXDelta="0%"
    android:toXDelta="-100%" />
'''
for p,c in files.items():
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(c, encoding='utf-8')
print('ok', len(files))
