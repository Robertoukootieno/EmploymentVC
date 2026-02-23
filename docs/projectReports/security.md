# Security Disclosure Policy

If you discover a security vulnerability in EmploymentVC, please report it to our security team responsibly.

## Reporting a Vulnerability

**DO NOT** open a public GitHub issue for security vulnerabilities.

### Contact Information
- **Email**: security@provenly.io
- **PGP Key**: [Available upon request]
- **Response Time**: We aim to acknowledge security reports within 24 hours

### Report Details
Please include:
1. Description of the vulnerability
2. Steps to reproduce
3. Potential impact
4. Affected versions
5. Your contact information

### Responsible Disclosure Timeline
1. **Day 1-3**: Initial acknowledgment and assessment
2. **Day 4-14**: Investigation and fix development
3. **Day 15-30**: Patch testing and release preparation
4. **Day 31**: Public disclosure after patch availability

### Vulnerability Scoring
We use CVSS 3.1 for severity assessment:
- **Critical (9.0-10.0)**: Immediate patch release
- **High (7.0-8.9)**: Patch release within 7 days
- **Medium (4.0-6.9)**: Patch release within 30 days
- **Low (0.1-3.9)**: Next scheduled release

## Security Best Practices for Users

### Application Security
1. **Keep dependencies updated**: Run `./gradlew dependencyUpdates` regularly
2. **Enable MFA**: Always enable two-factor authentication
3. **Rotate credentials**: Change passwords every 90 days
4. **Review access logs**: Monitor Loki logs for suspicious activity

### Deployment Security
1. **Use HTTPS**: Deploy with valid TLS certificates
2. **Enable audit logging**: Keep Postgres audit enabled
3. **Regular backups**: Daily automated database backups
4. **Network isolation**: Use VPC and security groups
5. **Access control**: Implement least privilege

### Operational Security
1. **Coordinate maintenance**: Announce maintenance windows
2. **Security patches**: Apply within 24-48 hours of release
3. **Monitoring**: Set up Prometheus alerts for anomalies
4. **Incident response**: Have runbooks for common issues

## Known Issues & Workarounds

Current known security issues and their mitigations:

### Issue 1: JWT Token Expiration
**Severity**: Medium  
**Description**: JWT tokens remain valid slightly longer than configured  
**Workaround**: Configure shorter token lifetimes (5 minutes)  
**Status**: Fixed in v1.0.2

### Issue 2: Credential Presenter Privacy
**Severity**: Low  
**Description**: Presentation metadata may reveal holder identity  
**Workaround**: Use credential presentation templates  
**Status**: Investigating for v1.1.0

## Security Headers

Ensure these headers are set by your reverse proxy:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'
Referrer-Policy: strict-origin-when-cross-origin
```

## Certificate Pinning

For mobile clients, consider implementing certificate pinning:

```java
// Example for mobile applications
CertificatePinner certificatePinner = new CertificatePinner.Builder()
    .add("api.provenly.io", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build();
```

## Credits

We appreciate security researchers who responsibly disclose vulnerabilities. Researchers will be credited in our security advisories upon request.

---

**Last Updated**: 2026-02-21  
**Policy Version**: 1.0
