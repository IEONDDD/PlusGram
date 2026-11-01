"""Check publishable sources and APK contents without printing matched secrets."""
import argparse
import io
import json
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PATTERNS = {
    'private signing key': rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
    'GitHub access token': rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{50,})',
    'Telegram bot token': rb'\b[0-9]{8,12}:[A-Za-z0-9_-]{35}\b',
    'personal Windows home path': rb'[A-Za-z]:[/\\]Users[/\\](?:Evgeniy|LeoNard)[/\\]',
}
FORBIDDEN = {'local.properties', 'release.keystore', 'google-services.json',
             'google-services.upstream.json', 'badges.json', '.env'}

def findings(name, data):
    return [{'file': name, 'kind': kind} for kind, pattern in PATTERNS.items()
            if re.search(pattern, data, re.I)]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    paths = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=ROOT).decode().split('\0')
    issues, checked, public_fixtures = [], 0, 0
    for name in sorted(set(paths)):
        path = ROOT/name
        if not path.is_file() or name == 'Tools/check_release_privacy.py': continue
        if path.name in FORBIDDEN or path.suffix in {'.jks', '.keystore', '.session'}:
            issues.append({'file': name, 'kind': 'private or retired file in publishable tree'})
        matches = findings(name, path.read_bytes())
        for match in matches:
            public_test = (name.startswith('TMessagesProj/jni/boringssl/') and
                           any(part in name for part in ['/test/', '/testdata/', '_test.', '/wycheproof_testvectors/'])) or name == 'TMessagesProj/jni/voip/webrtc/pc/test/fake_rtc_certificate_generator.h'
            if match['kind'] == 'private signing key' and public_test:
                public_fixtures += 1
            else:
                issues.append(match)
        checked += 1
    entries = 0
    if args.apk:
        def inspect_archive(archive, prefix, depth=0):
            nonlocal entries
            for item in archive.infolist():
                if item.is_dir(): continue
                entries += 1
                name = prefix + item.filename
                if Path(item.filename).name in FORBIDDEN or item.filename.endswith(('.session', '.keystore', '.jks')):
                    issues.append({'file': name, 'kind': 'private file packaged in APK'})
                data = archive.read(item)
                issues.extend(findings(name, data))
                if depth < 3 and zipfile.is_zipfile(io.BytesIO(data)):
                    with zipfile.ZipFile(io.BytesIO(data)) as nested:
                        inspect_archive(nested, name+'/', depth+1)
        with zipfile.ZipFile(args.apk) as apk:
            inspect_archive(apk, 'APK/')
    report = {'source_files_checked': checked, 'apk_entries_checked': entries,
              'public_crypto_test_fixtures': public_fixtures, 'findings': issues,
              'scope': 'Current publishable tree and optional APK; does not rewrite or audit old Git history. Telegram/Firebase client application identifiers are expected APK configuration.'}
    output = json.dumps(report, indent=2)
    if args.report: args.report.write_text(output+'\n', encoding='utf-8')
    print(output)
    return bool(issues)

if __name__ == '__main__':
    raise SystemExit(main())
