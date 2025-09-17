import React, { useState, useEffect } from 'react';
import {
  Button,
  Space,
  Loading,
  Input,
  Select,
  Message,
  Image,
  Card,
  Skeleton,
  Row,
  Col,
} from 'tdesign-react';
import useSWR from 'swr';
import './App.css';

const { Option } = Select;

interface MediaItem {
  id: number;
  name: string;
  url: string | null;
  originalName: string;
  uploadFrom: string;
  type: string;
  mimeType: string;
  title: string;
  caption: string;
  alternativeText: string;
  description: string;
  publicMedia: boolean;
  createdAt: number;
  updatedAt: number;
}

interface MediaResponse {
  items: MediaItem[];
  count: number;
  total: number;
  pageToken: {
    next: string;
  };
  continuation: {
    hasNext: boolean;
  };
}

const fetcher = (url: string) => fetch(url).then((res) => res.json());

const App = () => {
  const [keyword, setKeyword] = useState('');
  const [type, setType] = useState('');
  const [items, setItems] = useState<MediaItem[]>([]);
  const [fetchToken, setFetchToken] = useState<string>('');
  const [nextToken, setNextToken] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [total, setTotal] = useState(0);
  const [count, setCount] = useState(0);
  const [loadedImages, setLoadedImages] = useState<Set<number>>(new Set());

  const limit = 20; // ezyadmin.numberOfMediaPerPage

  const buildUrl = (token: string) => {
    let url = '/enhancement/api/v1/media/list'; // Simplified, adjust based on accessibleUris
    url += `?limit=${limit}`;
    if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
    if (type) url += `&type=${encodeURIComponent(type)}`;
    if (token && token !== '') url += `&nextPageToken=${token}`;
    return url;
  };

  const { data, error, isLoading, mutate } = useSWR<MediaResponse>(
    buildUrl(fetchToken),
    fetcher,
  );

  const handleSearch = () => {
    setFetchToken('');
    setItems([]);
    mutate();
  };

  const handleLoadMore = () => {
    if (hasNext && nextToken) {
      setFetchToken(nextToken);
    }
  };

  const getImageUrl = (item: MediaItem) => {
    return `/api/v1/media/${item.name}`;
  };

  if (error) {
    return <Message theme="error" content="Error loading media list" />;
  }

  return (
    <div className="content">
      <Space direction="vertical" style={{ width: '100%' }}>
        <h2>Media List</h2>
        <Space>
          <Input
            placeholder="Search keyword"
            value={keyword}
            onChange={setKeyword}
            onEnter={handleSearch}
          />
          <Select
            value={type}
            onChange={(value) => setType(value as string)}
            placeholder="Select type"
          >
            <Option value="" label="All" />
            <Option value="IMAGE" label="Image" />
            <Option value="VIDEO" label="Video" />
            {/* Add more options as needed */}
          </Select>
          <Button onClick={handleSearch}>Search</Button>
        </Space>

        <Space>
          <span>Count: {count}</span>
          <span>Total: {total}</span>
        </Space>

        {isLoading && items.length === 0 ? (
          <Loading />
        ) : (
          <Row gutter={16}>
            {data?.items.map((item) => (
              <Col>
                <Card key={item.id} style={{ width: '100%' }}>
                  {item.type === 'IMAGE' && loadedImages.has(item.id) && (
                    <Image
                      src={getImageUrl(item)}
                      srcset={{
                        'image/avif': '',
                        'image/webp': getImageUrl(item),
                      }}
                      alt={item.originalName}
                      loading={
                        <Skeleton
                          animation="gradient"
                          style={{
                            width: '100%',
                            height: '100%',
                          }}
                        />
                      }
                      fit="cover"
                      style={{
                        width: '100%',
                        height: '150px',
                      }}
                      onError={() => {
                        console.log('Image load error');
                      }}
                    />
                  )}
                  {item.type === 'IMAGE' && !loadedImages.has(item.id) && (
                    <Image
                      src={getImageUrl(item)}
                      alt={item.originalName}
                      loading="lazy"
                      fit="cover"
                      style={{
                        width: '100%',
                        height: '150px',
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        opacity: 0,
                      }}
                      onLoad={() => {
                        setLoadedImages((prev) => new Set(prev).add(item.id));
                      }}
                      onError={() => {
                        console.log('Image load error');
                        setLoadedImages((prev) => new Set(prev).add(item.id)); // Still hide skeleton
                      }}
                    />
                  )}
                  <div style={{ padding: '8px' }}>
                    <div style={{ fontWeight: 'bold', fontSize: '14px' }}>
                      {item.originalName}
                    </div>
                    <div style={{ fontSize: '12px', color: '#666' }}>
                      {item.mimeType} •{' '}
                      {new Date(item.createdAt).toLocaleDateString()}
                    </div>
                    {item.title && (
                      <div style={{ fontSize: '12px', marginTop: '4px' }}>
                        {item.title}
                      </div>
                    )}
                  </div>
                </Card>
              </Col>
            ))}
          </Row>
        )}

        {hasNext && (
          <div style={{ textAlign: 'center', marginTop: '16px' }}>
            <Button onClick={handleLoadMore} loading={isLoading}>
              Load More
            </Button>
          </div>
        )}
      </Space>
    </div>
  );
};

export default App;
