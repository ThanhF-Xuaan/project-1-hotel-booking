import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';

export function getAcceptancePostgresContainerId(): string {
  const containerId = execFileSync(
    'docker',
    [
      'ps',
      '--filter', 'label=com.docker.compose.project=hotel-m1-acceptance',
      '--filter', 'label=com.docker.compose.service=acceptance_postgres',
      '--format', '{{.ID}}',
    ],
    { encoding: 'utf-8' }
  ).trim();

  if (!containerId) {
    throw new Error('Acceptance PostgreSQL container is not running.');
  }

  return containerId;
}

export function getAcceptanceDatabaseDigest(): string {
  const dump = execFileSync(
    'docker',
    [
      'exec',
      getAcceptancePostgresContainerId(),
      'pg_dump',
      '--data-only',
      '--no-owner',
      '--no-privileges',
      '-U', 'postgres',
      '-d', 'hotel_acceptance_db',
    ],
    { maxBuffer: 64 * 1024 * 1024 }
  );

  // pg_dump emits a fresh psql \restrict token on each run; it is a dump guard, not DB data.
  const normalizedDump = dump.toString('utf-8').replace(/^\\(?:un)?restrict\s+\S+\r?$/gm, '');
  return createHash('sha256').update(normalizedDump).digest('hex');
}

export function queryAcceptanceDatabase(sql: string): string {
  return execFileSync(
    'docker',
    [
      'exec',
      getAcceptancePostgresContainerId(),
      'psql',
      '-U', 'postgres',
      '-d', 'hotel_acceptance_db',
      '-v', 'ON_ERROR_STOP=1',
      '-t',
      '-A',
      '-c', sql,
    ],
    { encoding: 'utf-8' }
  ).trim();
}
