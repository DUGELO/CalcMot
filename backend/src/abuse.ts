export interface AbuseConfig {
  ABUSE_PROTECTION?: string;
  ABUSE_REQUESTS_PER_MINUTE?: string;
  ABUSE_READS_PER_UID_PER_MINUTE?: string;
  ENTITLEMENT_READ_ENABLED?: string;
}

/** Bounded burst protection per isolate; not a distributed/account quota. No storage/service. */
export class AbuseGuard {
  private minute = -1;
  private requests = 0;
  private readonly owners = new Map<string, number>();

  private limit(value: string | undefined): number | null {
    if (!value || !/^[1-9][0-9]{0,5}$/.test(value)) return null;
    return Number(value);
  }

  private reset(now: number): void {
    const minute = Math.floor(now / 60000);
    if (minute !== this.minute) {
      this.minute = minute; this.requests = 0; this.owners.clear();
    }
  }

  request(config: AbuseConfig, now: number): boolean {
    if (config.ENTITLEMENT_READ_ENABLED === 'false') return false;
    if (!config.ABUSE_PROTECTION) return true; // Historical local configuration; explicit deployment gate.
    if (config.ABUSE_PROTECTION !== 'enforced') return false;
    const limit = this.limit(config.ABUSE_REQUESTS_PER_MINUTE);
    if (limit === null || this.limit(config.ABUSE_READS_PER_UID_PER_MINUTE) === null) return false;
    this.reset(now);
    return ++this.requests <= limit;
  }

  read(config: AbuseConfig, project: string, uid: string, now: number): boolean {
    if (!config.ABUSE_PROTECTION) return true;
    if (config.ABUSE_PROTECTION !== 'enforced') return false;
    const limit = this.limit(config.ABUSE_READS_PER_UID_PER_MINUTE);
    if (limit === null) return false;
    this.reset(now);
    const key = JSON.stringify([project, uid]); // Verified identity only; cleared on the next window rotation.
    const count = this.owners.get(key) ?? 0;
    // No eviction loophole: when full, reject new owners until the next window.
    if (!this.owners.has(key) && this.owners.size >= 512) return false;
    this.owners.set(key, count + 1);
    return count < limit;
  }
}
