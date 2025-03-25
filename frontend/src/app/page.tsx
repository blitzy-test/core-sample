import React from "react";
import { Link } from "react-router-dom";

/**
 * Home page component that serves as the landing page for the application
 * Provides navigation links to key features including the payments module
 */
function Home() {
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
        </ol>

        {/* Payment Module Navigation Section */}
        <div className="mt-8 w-full">
          <h2 className="text-xl font-semibold mb-4">Payment Module</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Link 
              to="/payments" 
              className="flex items-center p-4 border border-gray-200 rounded-md hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors"
            >
              <div className="mr-4 p-2 bg-blue-100 dark:bg-blue-900 rounded-full">
                <span className="text-blue-600 dark:text-blue-300 text-xl">[$]</span>
              </div>
              <div>
                <h3 className="font-medium">Payment Transactions</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">View and manage payment transactions</p>
              </div>
            </Link>
            
            <Link 
              to="/payments/new" 
              className="flex items-center p-4 border border-gray-200 rounded-md hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800 transition-colors"
            >
              <div className="mr-4 p-2 bg-green-100 dark:bg-green-900 rounded-full">
                <span className="text-green-600 dark:text-green-300 text-xl">[+]</span>
              </div>
              <div>
                <h3 className="font-medium">Create Payment</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400">Initialize a new payment transaction</p>
              </div>
            </Link>
          </div>
        </div>
      </main>
      <footer className="row-start-3 flex gap-[24px] flex-wrap items-center justify-center">
      </footer>
    </div>
  );
}

export default Home;