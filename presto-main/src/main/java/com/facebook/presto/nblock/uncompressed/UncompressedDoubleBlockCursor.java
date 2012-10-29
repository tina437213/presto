package com.facebook.presto.nblock.uncompressed;

import com.facebook.presto.Range;
import com.facebook.presto.Tuple;
import com.facebook.presto.TupleInfo;
import com.facebook.presto.nblock.BlockCursor;
import com.facebook.presto.slice.Slice;
import com.google.common.base.Preconditions;

import java.util.NoSuchElementException;

import static com.facebook.presto.SizeOf.SIZE_OF_DOUBLE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class UncompressedDoubleBlockCursor
        implements BlockCursor
{
    private final Slice slice;
    private final Range range;
    private long position = -1;
    private int offset = -1;

    public UncompressedDoubleBlockCursor(UncompressedBlock block)
    {
        this(checkNotNull(block, "block is null").getSlice(), block.getRange());
    }

    public UncompressedDoubleBlockCursor(Slice slice, Range range)
    {
        this.slice = slice;
        this.range = range;
    }

    @Override
    public TupleInfo getTupleInfo()
    {
        return TupleInfo.SINGLE_DOUBLE;
    }

    @Override
    public Range getRange()
    {
        return range;
    }

    @Override
    public boolean isValid()
    {
        return range.contains(position);
    }

    @Override
    public boolean isFinished()
    {
        return position > range.getEnd();
    }

    private void checkReadablePosition()
    {
        if (position > range.getEnd()) {
            throw new NoSuchElementException("already finished");
        }
        checkState(position >= range.getStart(), "cursor not yet advanced");
    }

    @Override
    public boolean advanceNextValue()
    {
        // every position is a new value
        if (position >= range.getEnd()) {
            position = Long.MAX_VALUE;
            return false;
        }

        if (position < 0) {
            position = range.getStart();
            offset = 0;
        } else {
            position++;
            offset += SIZE_OF_DOUBLE;
        }
        return true;
    }

    @Override
    public boolean advanceNextPosition()
    {
        return advanceNextValue();
    }

    @Override
    public boolean advanceToPosition(long newPosition)
    {
        Preconditions.checkArgument(newPosition >= this.position, "Can't advance backwards");

        if (newPosition > range.getEnd()) {
            position = Long.MAX_VALUE;
            return false;
        }

        // advance to specified position
        position = Math.max(newPosition, this.range.getStart());
        offset = (int) ((position - this.range.getStart()) * SIZE_OF_DOUBLE);
        return true;
    }

    @Override
    public long getPosition()
    {
        checkReadablePosition();
        return position;
    }

    @Override
    public long getCurrentValueEndPosition()
    {
        checkReadablePosition();
        return position;
    }

    @Override
    public Tuple getTuple()
    {
        checkReadablePosition();
        return new Tuple(slice.slice(offset, SIZE_OF_DOUBLE), TupleInfo.SINGLE_DOUBLE);
    }

    @Override
    public long getLong(int field)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int field)
    {
        checkReadablePosition();
        Preconditions.checkElementIndex(0, 1, "field");
        return slice.getDouble(offset);
    }

    @Override
    public Slice getSlice(int field)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean currentTupleEquals(Tuple value)
    {
        checkReadablePosition();
        Slice tupleSlice = value.getTupleSlice();
        return tupleSlice.length() == SIZE_OF_DOUBLE && slice.getDouble(offset) == tupleSlice.getDouble(0);
    }
}
