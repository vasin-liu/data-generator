// format-phone — script UDF body (GraalJS), one argument per call.
//
// The body runs inside the sandboxed GraalJsScriptUdfExecutor: it receives an `args` array and
// must `return` a scalar. When published as a SCRIPT UDF it is exposed as the SQL-callable
// function V2_FORMAT_PHONE(phone). It normalizes a phone number to a digits-only string and
// contains no host-escaping tokens, so it passes the publish-time governance scan.
if (args[0] === null || args[0] === undefined) {
  return null;
}
return String(args[0]).replace(/[^0-9]/g, '');
