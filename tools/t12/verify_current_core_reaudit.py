"""Compose the immutable pre-PR33 audit with the explicit reviewed PR41 deltas."""
import json
import verify_core_reaudit as core
import verify_widget_source_reaudit as widget


def verify_current():
    # Checks current worktree bytes, reviewed commit/hash/patch, all frozen artifacts,
    # new ErrorCode tuple preservation and the current transitive read dependencies.
    reviewed = widget.verify_reconciliation()

    def source(path):
        if path in (core.ERRORS, core.AVAILABILITY):
            return core.git('show', f'{core.ACCEPTED}:{path}')
        if path in core.U14_SOURCE_PATHS and path in widget.PRIOR_COMMITS:
            return core.git('show', f'{widget.PRIOR_COMMITS[path]}:{path}')
        return (core.ROOT / path).read_bytes()

    def patch(comparison, path):
        revision = widget.PRIOR_COMMITS.get(path)
        args = ['diff', '--no-ext-diff', '--binary', comparison]
        if revision:
            args.append(revision)
        return core.git(*args, '--', path)

    historical = core.verify_audit(read_working=source, read_u14_patch=patch)
    return {'status': 'PASS', 'scope': 'SOURCE_PROVENANCE_ONLY',
            'historicalCore': historical, 'reviewedCurrent': reviewed}


if __name__ == '__main__':
    print(json.dumps(verify_current(), indent=2))
