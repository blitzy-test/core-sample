import { Link } from 'react-router-dom';
import { getPaymentRoutePath } from './routes';

export default function Home() {
  return (
    <div className="grid grid-rows-[20px_1fr_20px] items-center justify-items-center min-h-screen p-8 pb-20 gap-16 sm:p-20 font-geist-sans">
      <main className="flex flex-col gap-[32px] row-start-2 items-center sm:items-start">
        <img
          className="dark:invert"
          src="/next.svg"
          alt="Next.js logo"
          width={180}
          height={38}
        />
        <ol className="list-inside list-decimal text-sm/6 text-center sm:text-left font-geist-mono">
          <li className="mb-2 tracking-[-.01em]">
            Get started by editing{" "}
            <code className="bg-black/[.05] dark:bg-white/[.06] px-1 py-0.5 rounded font-geist-mono font-semibold">
              src/app/page.tsx
            </code>
            .
          </li>
          <li className="mb-2 tracking-[-.01em]">
            Save and see your changes instantly.
          </li>
          <li className="tracking-[-.01em]">
            Explore the payment module features.
          </li>
        </ol>

        {/* Payment Module Navigation Links */}
        <div className="mt-8 p-6 border border-gray-200 dark:border-gray-800 rounded-lg w-full max-w-md">
          <h2 className="text-xl font-semibold mb-4 flex items-center">
            <span className="mr-2">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd" />
              </svg>
            </span>
            Payment Module
          </h2>
          <ul className="space-y-3">
            <li>
              <Link 
                to={getPaymentRoutePath()} 
                className="flex items-center p-3 text-base font-medium rounded-lg bg-gray-50 hover:bg-gray-100 dark:bg-gray-700 dark:hover:bg-gray-600 group hover:shadow transition-all"
              >
                <span className="flex-shrink-0 w-5 h-5 text-gray-500 dark:text-gray-400 group-hover:text-primary">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
                  </svg>
                </span>
                <span className="ml-3 flex-1 whitespace-nowrap">All Payments</span>
                <span className="inline-flex items-center justify-center px-2 py-0.5 ml-3 text-xs font-medium rounded bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-300">
                  View All
                </span>
              </Link>
            </li>
            <li>
              <Link 
                to={getPaymentRoutePath('default')} 
                className="flex items-center p-3 text-base font-medium rounded-lg bg-gray-50 hover:bg-gray-100 dark:bg-gray-700 dark:hover:bg-gray-600 group hover:shadow transition-all"
              >
                <span className="flex-shrink-0 w-5 h-5 text-gray-500 dark:text-gray-400 group-hover:text-primary">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M6 6V5a3 3 0 013-3h2a3 3 0 013 3v1h2a2 2 0 012 2v3.57A22.952 22.952 0 0110 13a22.95 22.95 0 01-8-1.43V8a2 2 0 012-2h2zm2-1a1 1 0 011-1h2a1 1 0 011 1v1H8V5zm1 5a1 1 0 011-1h.01a1 1 0 110 2H10a1 1 0 01-1-1z" clipRule="evenodd" />
                    <path d="M2 13.692V16a2 2 0 002 2h12a2 2 0 002-2v-2.308A24.974 24.974 0 0110 15c-2.796 0-5.487-.46-8-1.308z" />
                  </svg>
                </span>
                <span className="ml-3 flex-1 whitespace-nowrap">Default Organization</span>
                <span className="inline-flex items-center justify-center px-2 py-0.5 ml-3 text-xs font-medium rounded bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-300">
                  Organization
                </span>
              </Link>
            </li>
            <li>
              <Link 
                to={getPaymentRoutePath('default', 'primary')} 
                className="flex items-center p-3 text-base font-medium rounded-lg bg-gray-50 hover:bg-gray-100 dark:bg-gray-700 dark:hover:bg-gray-600 group hover:shadow transition-all"
              >
                <span className="flex-shrink-0 w-5 h-5 text-gray-500 dark:text-gray-400 group-hover:text-primary">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                    <path fillRule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clipRule="evenodd" />
                  </svg>
                </span>
                <span className="ml-3 flex-1 whitespace-nowrap">Primary Account</span>
                <span className="inline-flex items-center justify-center px-2 py-0.5 ml-3 text-xs font-medium rounded bg-primary/10 text-primary dark:bg-primary/20 dark:text-primary-light">
                  Account
                </span>
              </Link>
            </li>
          </ul>
          <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              The payment module provides comprehensive transaction management with filtering, sorting, and detailed views.
            </p>
          </div>
        </div>
      </main>
      <footer className="row-start-3 flex gap-[24px] flex-wrap items-center justify-center">
      </footer>
    </div>
  );
}