import { useState } from 'react';
import useSWR from 'swr';
import { Message, Select } from 'tdesign-react';
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
  const [fetchToken, setFetchToken] = useState<string>('');

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

  const getImageUrl = (item: MediaItem) => {
    return `/enhancement/api/v1/media/${item.name}`;
  };

  if (error) {
    return <Message theme="error" content="Error loading media list" />;
  }

  return <div className="content"></div>;
};

export default App;
