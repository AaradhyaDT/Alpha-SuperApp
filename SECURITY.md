# Security Policy

## Version

This security policy applies to version 1.1.0 and later. For older versions, refer to historical releases.

## Reporting a Vulnerability

At Alpha SuperApp, we take security seriously. If you discover a security vulnerability, please **do not** create a public GitHub issue. Instead, please follow our responsible disclosure process:

### How to Report

1. **Do not** publicly disclose the vulnerability
2. Email security details to: aaradhyadevtmr@gmail.com
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if available)

### What to Expect

- Acknowledgment of your report within 48 hours
- Regular updates on the status of the fix
- Credit in security advisories (if desired)
- Coordinated disclosure timeline

## Security Practices

### Code Security

- All code undergoes security review before merge
- Dependencies are regularly scanned for vulnerabilities
- Security patches are applied promptly

### Dependency Management

- Dependencies are managed through `gradle/libs.versions.toml`
- Regular updates for security patches
- Use `./gradlew dependencyUpdates` to check for outdated dependencies

### Build Security

- Release builds use signing configurations
- No credentials are committed to version control
- Use `.gitignore` to exclude sensitive files

### API Security

- Never commit API keys or secrets
- Use secure storage (Keystore, Encrypted Preferences)
- Validate all inputs
- Use HTTPS for network communications

## Sensitive Information

**Never commit:**
- API keys or tokens
- Private signing keys
- Database credentials
- Personal access tokens
- Configuration with sensitive data

Excluded via `.gitignore`:
- `local.properties`
- `.gradle/` directory
- IDE-specific files

## Dependency Vulnerabilities

Keep dependencies updated:

```bash
./gradlew dependencyUpdates
```

Monitor for security advisories in:
- [Gradle Dependency Vulnerabilities](https://github.com/advisories)
- Maven Central Security Advisories
- Library-specific security pages

## Compliance

- Follow Android Security & Privacy Year (ASPY) guidelines
- Comply with GDPR if handling user data
- Regular security audits

## Android Security

- Implement proper permission handling
- Use the latest Android security patches
- Follow Google Play security requirements
- Validate SSL/TLS certificates

## Questions?

If you have security-related questions (not disclosing vulnerabilities):
- Check Android Security & Privacy Documentation
- Review Google Play Security Policy
- Contact: Aaradhya Dev Tamrakar (aaradhyadevtmr@gmail.com)

---

**Last Updated**: March 2026
