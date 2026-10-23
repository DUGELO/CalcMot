export function assertDevBinding(config: unknown): {binding:string;database_id:string;database_name:string};
export function openDevBinding(): Promise<{db:D1Database;dispose():Promise<void>}>;
