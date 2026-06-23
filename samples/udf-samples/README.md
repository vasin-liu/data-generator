# Sample UDFs

One sample User-Defined Function per registry type, for exercising the upload → publish → reference → run loop
on the operator console (`/udfs`) or via the REST API (`/api/console/udfs`). All samples are credential-free.

> Registry SQL **and** SCRIPT UDFs are GraalJS-backed functions exposed to Calcite as SQL callables. The
> `script`/`sql` body receives an `args` array and must `return` a scalar. SCRIPT UDFs additionally require a
> non-empty input/output JSON Schema at publish time; SQL UDFs do not.

## script — `format-phone.js`

- **udfId:** `com.example.udf.formatphone`
- **version:** `1.0.0`
- **type:** `script`
- **sqlName (SQL function):** `V2_FORMAT_PHONE`
- **schema:** `format-phone.schema.json` (single string argument → digits-only string)
- **behavior:** normalizes a phone number to digits only, e.g. `+1 (555) 123-4567` → `15551234567`

## sql — `mask-email.sql`

- **udfId:** `com.example.udf.maskemail`
- **version:** `1.0.0`
- **type:** `sql`
- **sqlName (SQL function):** `V2_MASK_EMAIL`
- **behavior:** masks the local part of an email to its first character + `***`, e.g.
  `alice@example.com` → `a***@example.com`

## java-plugin

The java-plugin UDF sample is the existing PF4J sample at [`samples/template-v2-pf4j-plugin/`](../template-v2-pf4j-plugin/).
It is reused unchanged (java plugins load through the directory/PF4J path, not a parallel classloader), so no
duplicate java sample is added here.

## Referencing from a Template V2

Once published, reference the functions from a SQL transform by their `sqlName`:

```sql
SELECT id,
       V2_MASK_EMAIL(email)  AS masked_email,
       V2_FORMAT_PHONE(phone) AS clean_phone
FROM people
```

No real secrets, credentials, or host-escaping code appear in any sample.
