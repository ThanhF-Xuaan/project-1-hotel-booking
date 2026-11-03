"""Compose the immutable pre-PR33 audit with the explicit reviewed PR41 deltas."""
import json
import verify_core_reaudit as core
import verify_widget_source_reaudit as widget
from reconcile_deploy_source import reconcile as reconcile_deploy
from reconcile_m1_review_fixes import reconcile as reconcile_review_fixes, verify_current as verify_review_fixes


def verify_current():
    # Checks current worktree bytes, reviewed commit/hash/patch, all frozen artifacts,
    # new ErrorCode tuple preservation and the current transitive read dependencies.
    fixes = verify_review_fixes()
    reviewed = widget.verify_reconciliation()

    def source(path):
        if path == widget.U22_APP:
            return widget.reconcile_u22((core.ROOT / path).read_bytes())
        if path in (core.ERRORS, core.AVAILABILITY):
            return core.git('show', f'{core.ACCEPTED}:{path}')
        if path in core.U14_SOURCE_PATHS and path in widget.PRIOR_COMMITS:
            return core.git('show', f'{widget.PRIOR_COMMITS[path]}:{path}')
        return reconcile_deploy(path, reconcile_review_fixes(path, (core.ROOT / path).read_bytes()))

    def patch(comparison, path):
        revision = '35fb85a830b2f2315469faf289b303f795bf9380' if path == widget.U22_APP else widget.PRIOR_COMMITS.get(path)
        args = ['diff', '--no-ext-diff', '--binary', comparison]
        if revision:
            args.append(revision)
        return core.git(*args, '--', path)

    historical = core.verify_audit(read_working=source, read_u14_patch=patch)
    return {'status': 'PASS', 'scope': 'SOURCE_PROVENANCE_ONLY',
            'historicalCore': historical, 'reviewedCurrent': reviewed, 'reviewFixes': fixes}


if __name__ == '__main__':
    print(json.dumps(verify_current(), indent=2))
