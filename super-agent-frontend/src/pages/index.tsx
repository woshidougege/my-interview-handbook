import React from 'react';
import Head from 'next/head';
import AccountOverview from '@/components/AccountOverview';

const HomePage: React.FC = () => {
  return (
    <>
      <Head>
        <title>账户总览 - Super Agent</title>
        <meta name="description" content="Super Agent 用户账户总览页面" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      
      <AccountOverview />
    </>
  );
};

export default HomePage;
