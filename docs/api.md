# API

RDP provides an extensive RESTful API to perform gene searches, obtain user details, etc.

## Authentication

All API endpoints can be authorized with a secret token that can be generated by creating
a [service account](service-accounts.md) or listed under `rdp.settings.isearch.auth-tokens`.

```http
GET /api HTTP/1.1
Authorization: Bearer {accessToken}
```

Passing the authorization token via `auth` query parameter is deprecated as of 1.4.0.

Keep in mind that the token you use is tied to a user that has permissions.
What you can see through the API will be restricted to what the corresponding
user is authorized to read.

## List all users

List all users in a paginated format.

```http
GET /api/users HTTP/1.1
```

- `page` the page to query starting from zero to `totalPages`

If `rdp.settings.privacy.enable-anonymized-search-results` is set to `true`, anonymized results are included in the
output.

## List all genes

List all genes in a paginated format.

```http
GET /api/genes HTTP/1.1
```

- `page` the page to query starting from zero to `totalPages`

If `rdp.settings.privacy.enable-anonymized-search-results` is set to `true`, anonymized results are included in the
output.

## Search users

Search users by name or description.

```http
GET /api/users/search HTTP/1.1
```

By name:

- `nameLike` a name in the user profile
- `prefix` a boolean `true` or `false` as to whether the name search is matching a prefix

By description:

- `descriptionLike` a description in the user profile

In any case, you can narrow down the result with the following query parameters:

- `researcherPositions` a set of researcher positions, only
  `PRINCIPAL_INVESTIGATOR` is currently supported
- `researcherCategories` a set of researcher category like `IN_SILICO` or `IN_VITRO`
- `organUberonIds` a set of Uberon identifiers that the user has added to its profile as organ systems

## Search genes

Search user genes by symbols.

```http
GET /api/genes/search HTTP/1.1
```

- `symbol` a gene symbol
- `taxonId` a taxon identifier in which the search is performed
- `orthologTaxonId` an ortholog taxon identifier, omitting it searches all taxa
- `tiers` a set of tiers including `TIER1`, `TIER2` and `TIER3`

Then any of the following as described in "Search users":

- `researcherPositions`
- `researcherCategories`
- `organUberonIds`

The `tier` query parameter is deprecated as of 1.4.0.

## Find a user by its identifier

Find a user given its real or anonymous identifier.

```http
GET /api/users/{userId} HTTP/1.1
```

Or anonymously:

```http
GET /api/users/by-anonymous-id/{anonymousId} HTTP/1.1
```

The anonymous identifier can be obtained by performing a users or genes search and retrieving the `anonymousId` from its
result.

## Statistics

Obtain a few statistics for the number of registered users, genes, etc.

```http
GET /api/stats HTTP/1.1
```
