import { CheckCircle2 } from "lucide-react";

export default function AuthLayout({
                                     children,
                                   }: {
  children: React.ReactNode;
}) {
  return (
      <main className="grid min-h-dvh bg-background lg:grid-cols-[minmax(420px,0.86fr)_1.14fr]">
        <section className="relative hidden overflow-hidden border-r border-border bg-foreground text-background lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16">
          <div className="max-w-lg">
            <p className="mb-6 text-xs font-medium uppercase tracking-[0.18em] text-background/55">
              Lorem ipsum dolor sit amet
            </p>

            <h1 className="max-w-md text-4xl font-semibold leading-[1.08] tracking-[-0.05em] xl:text-5xl">
              Lorem ipsum dolor sit amet consectetur adipiscing elit.
            </h1>

            <p className="mt-6 max-w-md text-base leading-7 text-background/62">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do
              eiusmod tempor incididunt ut labore et dolore magna aliqua.
            </p>

            <div className="mt-10 flex flex-col gap-4 border-t border-background/15 pt-7">
              {[
                "Lorem ipsum dolor sit amet consectetur",
                "Adipiscing elit sed do eiusmod",
                "Tempor incididunt ut labore et dolore",
              ].map((item) => (
                  <div
                      key={item}
                      className="flex items-center gap-3 text-sm text-background/78"
                  >
                    <CheckCircle2 className="size-4" />
                    {item}
                  </div>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between border-t border-background/15 pt-6 text-xs text-background/45">
            <span>© 2026 Lorem Ipsum</span>
            <span>Lorem ipsum dolor sit amet</span>
          </div>
        </section>

        <section className="flex min-h-dvh items-center justify-center px-5 py-10 sm:px-8 lg:px-12">
          <div className="w-full max-w-[440px]">{children}</div>
        </section>
      </main>
  );
}