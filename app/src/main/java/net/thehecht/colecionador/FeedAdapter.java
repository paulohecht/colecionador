package net.thehecht.colecionador;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.squareup.picasso.Picasso;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {


    private SortedList<DataSnapshot> dataset = new SortedList<DataSnapshot>(DataSnapshot.class, new SortedList.Callback<DataSnapshot>() {

        @Override
        public int compare(DataSnapshot data1, DataSnapshot data2) {
            return (int)(data2.child("created_at").getValue(Long.class) - data1.child("created_at").getValue(Long.class));
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(DataSnapshot oldItem, DataSnapshot newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(DataSnapshot item1, DataSnapshot item2) {
            return item1.getKey().equals(item2.getKey());
        }
    });


    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        ImageView imageView;
        ImageView liked;
        TextView likedCount;

        public ViewHolder(View v) {
            super(v);
            textView = (TextView) v.findViewById(R.id.text);
            imageView = (ImageView) v.findViewById(R.id.image);
            liked = (ImageView) v.findViewById(R.id.liked);
            likedCount = (TextView) v.findViewById(R.id.liked_count);

        }

        public void reset() {
            if (textView != null) textView.setText("");
            if (imageView != null) imageView.setImageResource(R.drawable.placeholder);
            if (liked != null) liked.setImageResource(R.drawable.ic_favorite_border_black_24dp);
            if (likedCount != null) likedCount.setText("");
        }

        public void render(final DataSnapshot data) {
            if (textView != null) textView.setText(data.child("text").getValue(String.class));
            if (imageView != null) {
                Picasso.with(itemView.getContext())
                        .load(data.child("image").getValue(String.class))
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(imageView);
            }
            if (liked != null) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (data.child("likes").hasChild(userId) && data.child("likes").child(userId).getValue(Boolean.class)) {
                    liked.setImageResource(R.drawable.ic_favorite_black_24dp);
                    liked.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseDatabase.getInstance().getReference("posts").child(data.getKey()).runTransaction(new Transaction.Handler() {
                                @Override
                                public Transaction.Result doTransaction(MutableData mutableData) {
                                    int likesCount = mutableData.hasChild("likes_count") ? mutableData.child("likes_count").getValue(Integer.class) : 0;
                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    mutableData.child("likes").child(userId).setValue(false);
                                    mutableData.child("likes_count").setValue(Math.max(likesCount - 1, 0));
                                    DatabaseReference likeReference = FirebaseDatabase.getInstance().getReference("post_likes").child(data.getKey()).child(userId);
                                    likeReference.setValue(false);
                                    return Transaction.success(mutableData);
                                }
                                @Override
                                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                }
                            });
                        }
                    });
                }
                else {
                    liked.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            liked.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    FirebaseDatabase.getInstance().getReference("posts").child(data.getKey()).runTransaction(new Transaction.Handler() {
                                        @Override
                                        public Transaction.Result doTransaction(MutableData mutableData) {
                                            int likesCount = mutableData.hasChild("likes_count") ? mutableData.child("likes_count").getValue(Integer.class) : 0;
                                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                            mutableData.child("likes").child(userId).setValue(true);
                                            mutableData.child("likes_count").setValue(Math.max(likesCount + 1, 0));
                                            DatabaseReference likeReference = FirebaseDatabase.getInstance().getReference("post_likes").child(data.getKey()).child(userId);
                                            likeReference.setValue(true);
                                            return Transaction.success(mutableData);
                                        }

                                        @Override
                                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
            int likesCount = data.child("likes_count").getValue(Integer.class);
            if (likedCount != null && likesCount > 0) likedCount.setText("" + likesCount);

        }
    }

    public FeedAdapter(Context context) {
    }

    public void addItem(DataSnapshot dataSnapshot) {
        dataset.add(dataSnapshot);
        notifyDataSetChanged();
    }

    public void removeItem(DataSnapshot dataSnapshot) {
        dataset.remove(dataSnapshot);
        notifyDataSetChanged();
    }

    @Override
    public FeedAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main, parent, false));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.imageView != null) holder.imageView.setImageResource(R.drawable.placeholder);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.reset();
        DataSnapshot dataSnapshot = dataset.get(position);
        holder.render(dataSnapshot);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

}