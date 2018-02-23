package pv.chatredes.usuario;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import pv.chatredes.R;

public class UsuarioRecyclerListener {
    private final RecyclerView recyclerView;
    private OnItemClickListener onItemClickListener;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (onItemClickListener != null) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(v);
                onItemClickListener.onItemClicked(recyclerView, holder.getAdapterPosition(), v);
            }
        }
    };

    private RecyclerView.OnChildAttachStateChangeListener mAttachListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(View view) {
            if (onItemClickListener != null) {
                view.setOnClickListener(mOnClickListener);
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {
        }
    };

    private UsuarioRecyclerListener(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.recyclerView.setTag(R.id.item_click_support, this);
        this.recyclerView.addOnChildAttachStateChangeListener(mAttachListener);
    }

    public static UsuarioRecyclerListener addTo(RecyclerView view) {
        UsuarioRecyclerListener support = (UsuarioRecyclerListener) view.getTag(R.id.item_click_support);
        if (support == null) {
            support = new UsuarioRecyclerListener(view);
        }
        return support;
    }

    public static UsuarioRecyclerListener removeFrom(RecyclerView view) {
        UsuarioRecyclerListener support = (UsuarioRecyclerListener) view.getTag(R.id.item_click_support);
        if (support != null) {
            support.detach(view);
        }
        return support;
    }

    public UsuarioRecyclerListener setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
        return this;
    }

    private void detach(RecyclerView view) {
        view.removeOnChildAttachStateChangeListener(mAttachListener);
        view.setTag(R.id.item_click_support, null);
    }

    public interface OnItemClickListener {

        void onItemClicked(RecyclerView recyclerView, int position, View v);
    }

}