/**
 * springdoc represents a Spring `Pageable` argument as one `pageable` object query parameter, but
 * Spring's actual runtime binding — and what the server expects on the wire — is flat `page`/
 * `size`/`sort` query params, not a nested `pageable[...]` structure. openapi-fetch types the query
 * strictly against the (cosmetically wrong) generated schema, so this cast is the one place that
 * gap is bridged rather than fighting the generator or hand-rolling an untyped client.
 */
export function pageableQuery(page = 0, size = 12, sort?: string | string[]) {
  return { page, size, sort } as unknown as { pageable: { page?: number; size?: number; sort?: string[] } };
}
