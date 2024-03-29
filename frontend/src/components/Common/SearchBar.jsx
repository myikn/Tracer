import React, { useState } from 'react';
import styles from './SearchBar.module.scss';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import Loading from '../../components/Loading';

export default function SearchBar() {
  // search bar 내에 들어갈 검색어 변수
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  // navigate 이용 링크하면서 props 전달
  const navigate = useNavigate();

  const handleChange = e => {
    setText(e.target.value);
  };

  const handleSubmit = async e => {
    e.preventDefault();
    try {
      setLoading(true);
      const response = await axios.post(`${process.env.REACT_APP_API_URL}/news/search`, {
        word: text,
        limit: 5,
        offset: 0,
        type: 0,
      });
      setLoading(false);
      navigate(`/searchresult`, { state: { result: response.data, text: text, startDate: null, EndDate: null } });
    } catch (error) {
      alert(error.message);
    }
  };
  return (
    <div>
      {loading ? <Loading /> : null}
      <form onSubmit={handleSubmit} style={{ display: 'flex' }}>
        <input className={styles.search__input} type="text" placeholder="Search" onChange={handleChange} />
      </form>
    </div>
  );
}
