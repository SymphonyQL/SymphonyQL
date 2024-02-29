import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './styles.module.css';

const features = [
    {
        imageUrl: 'img/start-quickly-and-scale.svg',
        title: <>Start Quickly and Scale</>,
        description: (
            <>
                Build your first GraphQL server in minutes and
                scale to production loads.
            </>
        ),
    },
    {
        imageUrl: 'img/functional-and-type-safe.svg',
        title: <>Functional and Type-safe</>,
        description: (
            <>
                Use the power of Functional Programming and the compiler
                to build robust, correct and fully-featured GraphQL servers.
            </>
        ),
    },
    {
        title: <>Stream with Pekko</>,
        imageUrl: 'img/stream-with-pekkostream.svg',
        description: (
            <>
                Use Pekko's feature-rich pekko-streams to create query, subscription.
            </>
        ),
    },
    {
        title: <>Highly Concurrent</>,
        imageUrl: 'img/highly-concurrent.svg',
        description: (
            <>
                Leverage the power of Pekko to build asynchronous servers.
            </>
        ),
    }
];

function Feature({imageUrl, title, description}) {
    const imgUrl = useBaseUrl(imageUrl);
    return (
        <div className={clsx('col col--4', styles.feature)}>
            {imgUrl && (
                <div className="text--center">
                    <img className={styles.featureImage} src={imgUrl} alt={title} />
                </div>
            )}
            <h3>{title}</h3>
            <p>{description}</p>
        </div>
    );
}

function Home() {
    const context = useDocusaurusContext();
    const {siteConfig = {}} = context;
    const img = useBaseUrl('img/logo.png');
    return (
        <Layout
            title={`A GraphQL implementation built with Apache Pekko`}
            description="SymphonyQL is a GraphQL implementation built with Apache Pekko.">
            <header className={clsx('hero hero--primary', styles.heroBanner)}>
                <div className="container">
                    <img src={img} width="10%"/>
                    <p className="hero__subtitle">{siteConfig.tagline}</p>
                    <div className={styles.buttons}>
                        <Link
                            className={clsx(
                                styles.indexCtasGetStartedButton,
                            )}
                            to={useBaseUrl('docs/')}>
                            Get Started
                        </Link>
                    </div>
                </div>
            </header>
            <main>
                {features && features.length > 0 && (
                    <section className={styles.features}>
                        <div className="container">
                            <div className="row">
                                {features.map((props, idx) => (
                                    <Feature key={idx} {...props} />
                                ))}
                            </div>
                        </div>
                    </section>
                )}
            </main>
        </Layout>
    );
}

export default Home;