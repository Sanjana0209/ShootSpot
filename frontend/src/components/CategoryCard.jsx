function CategoryCard({ title, description, icon }) {
  return (
    <article className="category-card">
      <div className="category-icon" aria-hidden="true">{icon}</div>
      <h3>{title}</h3>
      <p>{description}</p>
    </article>
  );
}

export default CategoryCard;
