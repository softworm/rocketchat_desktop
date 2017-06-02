package com.rc.adapter.message;

import com.rc.adapter.BaseAdapter;
import com.rc.adapter.ViewHolder;
import com.rc.entity.MessageItem;
import com.rc.forms.MainFrame;
import com.rc.utils.TimeUtil;

import java.util.List;

/**
 * Created by song on 17-6-2.
 */
public class MessageAdapter extends BaseAdapter<ViewHolder>
{
    private List<MessageItem> messageItems;

    public MessageAdapter(List<MessageItem> messageItems)
    {
        this.messageItems = messageItems;
    }

    @Override
    public int getItemViewType(int position)
    {
        return messageItems.get(position).getMessageType();
    }

    @Override
    public ViewHolder onCreateViewHolder(int viewType)
    {
        switch (viewType)
        {
            case MessageItem.RIGHT_TEXT:{
                return new MessageRightTextViewHolder();
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position)
    {
        if (viewHolder == null)
        {
            return;
        }

        final MessageItem item = messageItems.get(position);
        MessageItem preItem = position == 0 ? null : messageItems.get(position - 1);

        if (viewHolder instanceof MessageRightTextViewHolder)
        {
            processRightTextMessage(viewHolder, item);
        }
    }

    @Override
    public int getCount()
    {
        return messageItems.size();
    }

    /**
     * 处理 我发送的文本消息
     *
     * @param viewHolder
     * @param item
     */
    private void processRightTextMessage(ViewHolder viewHolder, final MessageItem item)
    {
        MessageRightTextViewHolder holder = (MessageRightTextViewHolder) viewHolder;
        holder.text.setText(item.getMessageContent());
        holder.time.setText(TimeUtil.diff(item.getTimestamp()));
        //processMessageContent(holder.messageText, item);
        //registerMessageTextListener(holder.messageText, item);

        // 判断是否显示重发按钮
        if (item.isNeedToResend())
        {
           /* holder.resendButton.setVisibility(View.VISIBLE);
            holder.messageSendingProgressBar.setVisibility(View.GONE);
            holder.resendButton.setTag(R.id.message_id, item.getId());*/
        }
        else
        {
            //holder.resendButton.setVisibility(View.GONE);
            // 如果是刚发送的消息，显示正在发送进度条
            if (item.getUpdatedAt() < 1)
            {
                //holder.messageSendingProgressBar.setVisibility(View.VISIBLE);
            }
            else
            {
                //holder.messageSendingProgressBar.setVisibility(View.GONE);
            }
        }
    }
}
