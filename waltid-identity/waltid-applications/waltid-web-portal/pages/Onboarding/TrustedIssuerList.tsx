export default function TrustedIssuerList({ issuers }: { issuers?: any[] }) {
  if (!Array.isArray(issuers) || issuers.length === 0) return null;

  return (
    <div className="mt-4">
      <h3 className="font-bold mb-2">Trusted Issuers</h3>
      <ul>
        {issuers.map((issuer, idx) => (
          <li key={idx} className="font-mono text-sm text-green-700">
            {issuer.did}
          </li>
        ))}
      </ul>
    </div>
  );
}
