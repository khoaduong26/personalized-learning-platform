export default function ProfileIcon({ name }) {
  const paths = {
    user: 'M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
    lock: 'M7 10V7a5 5 0 0 1 10 0v3M5 10h14v11H5zM12 14v3',
    camera: 'M8 5l2-3h4l2 3h5v15H3V5h5Zm8 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
    logout: 'M9 3H4v18h5M9 12h12m-5-5 5 5-5 5',
    arrow: 'M19 12H5m6-6-6 6 6 6',
    check: 'M5 12l4 4L19 6',
  }
  return <svg className="profile-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
