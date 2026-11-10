// SPDX-License-Identifier: AGPL-3.0-only
package com.lyra.blocker;

/** The same checks run under JUnit/Gradle and directly with a JDK when Android tools are unavailable. */
public final class PasswordCredentialChecks {
    public static void main(String[] args) throws Exception { run(); System.out.println("Password credential checks passed"); }

    public static void run() throws Exception {
        check(PasswordCredential.problem("short") != null, "Short password accepted");
        check(PasswordCredential.problem(" ".repeat(20)) != null, "Whitespace-only password accepted");
        check(PasswordCredential.problem("abababababab") != null, "Repeated characters accepted");
        check(PasswordCredential.problem("password123456") != null, "Obvious password accepted");
        check(PasswordCredential.problem("a".repeat(129)) != null, "Oversized password accepted");
        String phrase = "copper moon garden bicycle";
        check(PasswordCredential.problem(phrase) == null, "Passphrase rejected");
        check(PasswordCredential.problem("അക്ഷരങ്ങൾ നിറഞ്ഞൊരു പൂന്തോട്ടം") == null, "Unicode passphrase rejected");
        PasswordCredential.Hash first = PasswordCredential.create(phrase);
        PasswordCredential.Hash second = PasswordCredential.create(phrase);
        check(!first.salt.equals(second.salt), "Salts were reused");
        check(!first.digest.equals(second.digest), "Different salts produced the same digest");
        check(PasswordCredential.verify(phrase, first.salt, first.digest, PasswordCredential.ITERATIONS), "Correct password rejected");
        check(!PasswordCredential.verify("wrong password phrase", first.salt, first.digest, PasswordCredential.ITERATIONS), "Wrong password accepted");
        check(!PasswordCredential.verify(" " + phrase, first.salt, first.digest, PasswordCredential.ITERATIONS), "Password was silently trimmed");
        check(!PasswordCredential.verify(phrase, "malformed", first.digest, PasswordCredential.ITERATIONS), "Malformed salt accepted");
        check(!PasswordCredential.verify(phrase, first.salt, "bad", PasswordCredential.ITERATIONS), "Malformed digest accepted");
        check(!PasswordCredential.verify(phrase, null, null, PasswordCredential.ITERATIONS), "Missing record accepted");
        check(!PasswordCredential.verify(phrase, first.salt, first.digest, 1), "Downgraded iteration count accepted");

        check(PasswordCredential.problem("123456") != null, "Short numeric password accepted");
        check(!PasswordCredential.verify(phrase, first.salt, first.digest, 120_000), "Old iteration count accepted");
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
