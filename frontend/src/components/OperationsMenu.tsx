interface OperationItem {
  id: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  action: () => void;
  disabled?: boolean;
}

interface OperationsMenuProps {
  items: OperationItem[];
  isProcessing: boolean;
}

export default function OperationsMenu({ items, isProcessing }: OperationsMenuProps) {
  return (
    <div className="menu-grid">
      {items.map((item) => (
        <button
          key={item.id}
          onClick={item.action}
          disabled={item.disabled || isProcessing}
          className="menu-item"
        >
          <div className="menu-item-content">
            <item.icon className="menu-icon" />
            <div className="menu-text">
              <h4>{item.name}</h4>
              <p>{item.description}</p>
            </div>
          </div>
        </button>
      ))}
    </div>
  );
}
