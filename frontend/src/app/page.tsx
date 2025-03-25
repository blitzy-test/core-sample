import { Link } from 'react-router-dom';

export default function Home() {
  return (
    <div className="grid grid-rows-[20px_1fr_20px] items-center justify-items-center min-h-screen p-8 pb-20 gap-16 sm:p-20 font-[family-name:var(--font-geist-sans)]">
      <main className="flex flex-col gap-[32px] row-start-2 items-center sm:items-start">
        <img
          className="dark:invert"
          src="/next.svg"
          alt="Next.js logo"
          width={180}
          height={38}
        />
        <ol className="list-inside list-decimal text-sm/6 text-center sm:text-left font-[family-name:var(--font-geist-mono)]">
          <li className="mb-2 tracking-[-.01em]">
            Get started by editing{" "}
            <code className="bg-black/[.05] dark:bg-white/[.06] px-1 py-0.5 rounded font-[family-name:var(--font-geist-mono)] font-semibold">
              src/app/page.tsx
            </code>
            .
          </li>
          <li className="tracking-[-.01em]">
            Save and see your changes instantly.
          </li>
          <li className="mt-2 tracking-[-.01em]">
            <Link 
              to="/payments" 
              className="text-blue-600 dark:text-blue-400 hover:underline flex items-center"
            >
              <span className="mr-2">[$]</span> Manage Payments
            </Link>
          </li>
          <li className="mt-2 tracking-[-.01em]">
            <Link 
              to="/payments/transactions" 
              className="text-blue-600 dark:text-blue-400 hover:underline flex items-center"
            >
              <span className="mr-2">[$]</span> View Transactions
            </Link>
          </li>
        </ol>
      </main>
      <footer className="row-start-3 flex gap-[24px] flex-wrap items-center justify-center">
      </footer>
    </div>
  );
}