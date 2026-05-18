import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

import { api, setToken } from "@/lib/api";
import { cn } from "@/lib/cn";

const LoginSchema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});
type LoginForm = z.infer<typeof LoginSchema>;

interface LoginResponse {
  accessToken: string;
  user: { id: string; email: string; tenantId: string };
}

export function LoginPage() {
  const nav = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(LoginSchema),
  });
  const [submitError, setSubmitError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (form: LoginForm) =>
      api<LoginResponse>("/v1/auth/login", { method: "POST", body: JSON.stringify(form) }),
    onSuccess: (data) => {
      setToken(data.accessToken);
      nav({ to: "/" });
    },
    onError: (err: Error) => setSubmitError(err.message),
  });

  return (
    <div className="min-h-dvh grid place-items-center px-6">
      <form
        onSubmit={handleSubmit((data) => mutation.mutate(data))}
        className="w-full max-w-sm border border-border bg-card rounded-lg p-8 space-y-5"
      >
        <header className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Sign in</h1>
          <p className="text-sm text-muted-foreground">to your ebay-soft seller dashboard</p>
        </header>

        <Field label="Email" error={errors.email?.message}>
          <input
            type="email"
            autoComplete="email"
            className={cn(inputBase, errors.email && "border-negative")}
            {...register("email")}
          />
        </Field>

        <Field label="Password" error={errors.password?.message}>
          <input
            type="password"
            autoComplete="current-password"
            className={cn(inputBase, errors.password && "border-negative")}
            {...register("password")}
          />
        </Field>

        {submitError && (
          <p className="text-sm text-negative" role="alert">{submitError}</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full h-10 rounded-md bg-accent text-accent-foreground font-medium hover:opacity-90 disabled:opacity-50 transition"
        >
          {mutation.isPending ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </div>
  );
}

const inputBase =
  "w-full h-10 rounded-md border border-border bg-muted px-3 text-sm focus:outline-none focus:ring-2 focus:ring-accent/50";

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-xs uppercase tracking-wide text-muted-foreground">{label}</span>
      {children}
      {error && <span className="text-xs text-negative">{error}</span>}
    </label>
  );
}
