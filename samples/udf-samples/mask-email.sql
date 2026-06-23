// mask-email — SQL UDF body (executed by GraalJS), one argument per call.
//
// Registry SQL UDFs are GraalJS-backed functions exposed to Calcite as SQL callables. This body
// receives an `args` array and must `return` a scalar; when published it becomes the SQL-callable
// function V2_MASK_EMAIL(email). It masks the local part of an email to its first character plus
// "***", e.g. alice@example.com -> a***@example.com. SQL UDFs require no JSON Schema (unlike SCRIPT).
if (args[0] === null || args[0] === undefined) {
  return null;
}
return String(args[0]).replace(/^(.).*(@.*)$/, '$1***$2');
