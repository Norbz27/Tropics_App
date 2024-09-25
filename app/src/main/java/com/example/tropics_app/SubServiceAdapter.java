package com.example.tropics_app;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class SubServiceAdapter extends RecyclerView.Adapter<SubServiceAdapter.ViewHolder> {
    private List<Map<String, Object>> serviceList;
    private OnItemClickListener listener;
    private boolean removeDrawable;
    private SubServiceAdapter.OnItemLongClickListener longClickListener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> service);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }
    public SubServiceAdapter(Context context, List<Map<String, Object>> serviceList, OnItemClickListener listener, boolean removeDrawable) {
        this.context = context;
        this.serviceList = serviceList;
        this.listener = listener;
        this.removeDrawable = removeDrawable;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_sub_service_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> service = serviceList.get(position);
        holder.bind(service, listener);
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, holder.getAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void updateList(List<Map<String, Object>> newServiceList) {
        this.serviceList = newServiceList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tvService);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }

        void bind(Map<String, Object> service, OnItemClickListener listener) {
            tvServiceName.setText((String) service.get("sub_service_name"));

            // Safely retrieve and convert the price to a string
            Object priceObj = service.get("price");
            String price = "";

            if (priceObj != null) {
                // Convert the price object to a double and format it
                double priceValue;
                if (priceObj instanceof Number) {
                    priceValue = ((Number) priceObj).doubleValue();
                    price = String.format("₱%.2f", priceValue);
                }
            }

            // Remove drawable if applicable
            if (removeDrawable) {
                tvPrice.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            // Set price text or clear it
            if (!price.equals("₱0.00")) {
                tvPrice.setText(price);
            } else {
                tvPrice.setText("");
            }

            itemView.setOnClickListener(v -> listener.onItemClick(service));
        }



    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);

        // Set the gravity to the right
        popupMenu.setGravity(Gravity.END); // Use Gravity.RIGHT or Gravity.END based on your needs

        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.inventory_item_menu, popupMenu.getMenu());

        // Get the menu object and hide specific items
        Menu menu = popupMenu.getMenu();

        // Hide the 'add' and 'subtract' menu items if needed
        menu.findItem(R.id.action_add).setVisible(false);  // To hide 'add'
        menu.findItem(R.id.action_subtract).setVisible(false);  // To hide 'subtract'

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.action_edit) {
                // Call the edit action
                if (longClickListener != null) {
                    longClickListener.onEditClick(serviceList.get(position));
                }
                return true;
            } else if (id == R.id.action_delete) {
                // Call the delete action
                if (longClickListener != null) {
                    longClickListener.onDeleteClick(serviceList.get(position));
                }
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    // Interface for handling long-clicks with edit and delete actions
    public interface OnItemLongClickListener {
        void onEditClick(Map<String, Object> item);
        void onDeleteClick(Map<String, Object> item);
    }
}
