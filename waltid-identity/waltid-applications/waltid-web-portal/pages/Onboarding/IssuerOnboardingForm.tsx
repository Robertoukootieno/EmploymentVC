import { useState } from "react";
import { onboardIssuer } from "@/lib/api/onboardIssuer";

export default function IssuerOnboardingForm({ apiUrl, onOnboarded }: { apiUrl: string, onOnboarded: (issuer: any) => void }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleOnboard() {
    setLoading(true);
    setError(null);
    try {
      const issuerData = {
        key: { backend: "jwk", keyType: "secp256r1" },
        did: { method: "jwk" }
      };
      const result = await onboardIssuer(apiUrl, issuerData);
      onOnboarded(result);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mb-4">
      <button
        className="btn btn-primary"
        onClick={handleOnboard}
        disabled={loading}
      >
        {loading ? "Onboarding..." : "Onboard New Issuer"}
      </button>
      {error && <div className="text-red-500 mt-2">{error}</div>}
    </div>
  );
}