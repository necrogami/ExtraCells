package extracells.tile;

import java.util.ArrayList;

import cpw.mods.fml.common.network.PacketDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import appeng.api.IAEItemStack;
import appeng.api.IItemList;
import appeng.api.Util;
import appeng.api.WorldCoord;
import appeng.api.events.GridTileLoadEvent;
import appeng.api.events.GridTileUnloadEvent;
import appeng.api.me.tiles.IDirectionalMETile;
import appeng.api.me.tiles.IGridMachine;
import appeng.api.me.util.IGridInterface;
import appeng.api.networkevents.MENetworkEventSubscribe;
import appeng.api.networkevents.MENetworkStorageEvent;
import extracells.Extracells;
import extracells.SpecialFluidStack;

public class TileEntityTerminalFluid extends ColorableECTile implements IGridMachine, IDirectionalMETile
{
	Boolean powerStatus = true, networkReady = true;
	IGridInterface grid;
	private String costumName = StatCollector.translateToLocal("tile.block.fluid.terminal");
	private ItemStack[] slots = new ItemStack[3];
	private int fluidIndex = 0;
	ECPrivateInventory inventory = new ECPrivateInventory(slots, costumName, 64)
	{
		public boolean isItemValidForSlot(int i, ItemStack itemstack)
		{
			return FluidContainerRegistry.isContainer(itemstack) || (itemstack != null && itemstack.getItem() instanceof IFluidContainerItem);
		}
	};
	ArrayList<SpecialFluidStack> fluidsInNetwork = new ArrayList<SpecialFluidStack>();

	public void updateEntity()
	{
		if (!worldObj.isRemote && isMachineActive())
		{
			ItemStack input = getInventory().getStackInSlot(0);
			ItemStack output = getInventory().getStackInSlot(1);

			if (!fluidsInNetwork.isEmpty())
			{
				try
				{
					fluidsInNetwork.get(fluidIndex);
				} catch (IndexOutOfBoundsException e)
				{
					fluidIndex = 0;
				}

				if (fluidsInNetwork.get(fluidIndex) != null)
				{
					Fluid requestedFluid = fluidsInNetwork.get(fluidIndex).getFluid();
					ItemStack preview = new ItemStack(extracells.Extracells.FluidDisplay, 1, fluidsInNetwork.get(fluidIndex).getID());

					if (preview.getTagCompound() == null)
						preview.setTagCompound(new NBTTagCompound());
					NBTTagCompound nbt = preview.getTagCompound();
					nbt.setLong("amount", fluidsInNetwork.get(fluidIndex).amount);
					nbt.setString("fluidname", StatCollector.translateToLocal((FluidRegistry.getFluidName(new FluidStack(fluidsInNetwork.get(fluidIndex).getFluid(), 1)))));// (capitalizeFirstLetter(fluidsInNetwork.get(fluidIndex).fluid.getName()));

					getInventory().setInventorySlotContents(2, preview);

					if (input != null)
					{
						if (FluidContainerRegistry.isEmptyContainer(input))
						{
							FluidStack request = new FluidStack(requestedFluid, 1000);

							ItemStack filledContainer = FluidContainerRegistry.fillFluidContainer(request, input);

							if (filledContainer != null)
							{
								if (output == null)
								{
									if (drainFluid(request))
									{
										getInventory().setInventorySlotContents(1, FluidContainerRegistry.fillFluidContainer(request, input));
										getInventory().decrStackSize(0, 1);
									}
								} else if (output.isStackable() && output.stackSize < output.getMaxStackSize() && output.getItem() == filledContainer.getItem() && output.getItemDamage() == filledContainer.getItemDamage() && output.getTagCompound() == filledContainer.getTagCompound())
								{
									if (drainFluid(request))
									{
										output.stackSize = output.stackSize + 1;
										getInventory().decrStackSize(0, 1);
									}
								}
							}
						} else if (input.getItem() instanceof IFluidContainerItem)
						{
							ItemStack inputTemp = input.copy();
							inputTemp.stackSize = 1;

							IFluidContainerItem fluidContainerItem = (IFluidContainerItem) inputTemp.getItem();

							if (fluidContainerItem.getFluid(inputTemp) == null || fluidContainerItem.getFluid(inputTemp).amount == 0)
							{

								FluidStack request = new FluidStack(requestedFluid, fluidContainerItem.getCapacity(inputTemp));

								ItemStack inputToBeFilled = inputTemp.copy();
								inputToBeFilled.stackSize = 1;
								int filledAmount = fluidContainerItem.fill(inputToBeFilled, request, true);

								if (output == null)
								{
									if (drainFluid(request))
									{
										getInventory().setInventorySlotContents(1, inputToBeFilled);
										getInventory().decrStackSize(0, 1);
									}
								} else if (output != null && output.itemID == inputToBeFilled.itemID && (!inputToBeFilled.getHasSubtypes() || inputToBeFilled.getItemDamage() == output.getItemDamage()) && ItemStack.areItemStackTagsEqual(inputToBeFilled, output))
								{
									if (output.stackSize + inputToBeFilled.stackSize <= inputToBeFilled.getMaxStackSize())
									{
										if (drainFluid(request))
										{
											output.stackSize = output.stackSize + 1;
											getInventory().decrStackSize(0, 1);
										}
									}
								}
							}
						}
					}
				}
			} else
			{
				getInventory().setInventorySlotContents(2, null);
			}

			if (FluidContainerRegistry.isFilledContainer(input))
			{
				ItemStack drainedContainer = input.getItem().getContainerItemStack(input);
				FluidStack containedFluid = FluidContainerRegistry.getFluidForFilledItem(input);

				if (FluidContainerRegistry.getFluidForFilledItem(input) != null)
				{
					if (output == null)
					{
						if (fillFluid(FluidContainerRegistry.getFluidForFilledItem(input)))
						{
							getInventory().setInventorySlotContents(1, drainedContainer);
							getInventory().decrStackSize(0, 1);
						}
					} else if (output.isStackable() && output.stackSize < output.getMaxStackSize())
					{
						if (drainedContainer == null)
						{
							if (fillFluid(FluidContainerRegistry.getFluidForFilledItem(input)))
							{

								getInventory().decrStackSize(0, 1);
							}
						} else if (output.isStackable() && output.stackSize < output.getMaxStackSize() && output.getItem() == drainedContainer.getItem() && output.getItemDamage() == drainedContainer.getItemDamage() && output.getTagCompound() == drainedContainer.getTagCompound())
						{
							if (fillFluid(FluidContainerRegistry.getFluidForFilledItem(input)))
							{
								output.stackSize = output.stackSize + 1;
								getInventory().decrStackSize(0, 1);
							}
						}
					}
				}
			} else if (input != null && input.getItem() instanceof IFluidContainerItem)
			{
				ItemStack inputTemp = input.copy();
				inputTemp.stackSize = 1;

				IFluidContainerItem fluidContainerItem = (IFluidContainerItem) inputTemp.getItem();
				FluidStack containedFluid = fluidContainerItem.getFluid(inputTemp);

				if (containedFluid != null && containedFluid.amount > 0)
				{
					ItemStack inputToBeDrained = inputTemp.copy();
					inputToBeDrained.stackSize = 1;

					int drainedAmount = fluidContainerItem.drain(inputToBeDrained, containedFluid.amount, true).amount;

					if (output == null)
					{
						if (fillFluid(containedFluid))
						{
							getInventory().setInventorySlotContents(1, inputToBeDrained);
							getInventory().decrStackSize(0, 1);
						}
					} else if (output.isStackable() && output.stackSize < output.getMaxStackSize())
					{
						if (output != null && output.itemID == inputToBeDrained.itemID && (!inputToBeDrained.getHasSubtypes() || inputToBeDrained.getItemDamage() == output.getItemDamage()) && ItemStack.areItemStackTagsEqual(inputToBeDrained, output))
						{
							if (output.stackSize + inputToBeDrained.stackSize <= inputToBeDrained.getMaxStackSize())
							{
								if (fillFluid(containedFluid))
								{
									output.stackSize = output.stackSize + 1;
									getInventory().decrStackSize(0, 1);
								}
							}
						}
					}
				}
			}
		}
	}

	public ECPrivateInventory getInventory()
	{
		return inventory;
	}

	@MENetworkEventSubscribe
	public void networkUpdate(MENetworkStorageEvent e)
	{
		updateFluids(e.currentItems);
	}

	public void updateFluids(IItemList currentItems)
	{
		fluidsInNetwork = new ArrayList<SpecialFluidStack>();

		if (grid != null)
		{
			for (IAEItemStack itemstack : currentItems)
			{
				if (itemstack.getItem() == extracells.Extracells.FluidDisplay && itemstack.getStackSize() > 0)
				{
					fluidsInNetwork.add(new SpecialFluidStack(itemstack.getItemDamage(), itemstack.getStackSize()));
				}
			}
		}

		PacketDispatcher.sendPacketToAllPlayers(getDescriptionPacket());
	}

	public boolean fillFluid(FluidStack toImport)
	{
		IAEItemStack toFill = Util.createItemStack(new ItemStack(extracells.Extracells.FluidDisplay, toImport.amount, toImport.fluidID));
		if (grid != null)
		{
			IAEItemStack sim = grid.getCellArray().calculateItemAddition(Util.createItemStack(new ItemStack(Extracells.FluidDisplay, (int) (toFill.getStackSize()), toFill.getItemDamage())));

			if (sim != null)
			{
				return false;
			}

			grid.getCellArray().addItems(Util.createItemStack(new ItemStack(toFill.getItem(), (int) (toFill.getStackSize()), toFill.getItemDamage())));
			return true;
		}
		return false;
	}

	public boolean drainFluid(FluidStack toExport)
	{
		IAEItemStack toDrain = Util.createItemStack(new ItemStack(extracells.Extracells.FluidDisplay, toExport.amount, toExport.fluidID));
		if (grid != null)
		{
			for (SpecialFluidStack fluidstack : fluidsInNetwork)
			{
				if (fluidstack.getFluid() == toExport.getFluid() && fluidstack.amount >= toExport.amount)
				{
					IAEItemStack takenStack = grid.getCellArray().extractItems(Util.createItemStack(new ItemStack(toDrain.getItem(), (int) (toDrain.getStackSize()), toDrain.getItemDamage())));

					if (takenStack == null)
					{
						return false;
					} else if (takenStack.getStackSize() != (int) toDrain.getStackSize())
					{
						grid.getCellArray().addItems(takenStack);
						return false;
					} else
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public void setCurrentFluid(int modifier)
	{
		if (fluidIndex + modifier >= 0)
		{
			this.fluidIndex = fluidIndex + modifier;
		} else
		{
			this.fluidIndex = this.fluidsInNetwork.size() - 1;
		}
	}

	public int getCurrentFluid()
	{
		return fluidIndex;
	}

	public String capitalizeFirstLetter(String original)
	{
		if (original.length() == 0)
			return original;
		return original.substring(0, 1).toUpperCase() + original.substring(1);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		NBTTagList nbttaglist = new NBTTagList();

		for (int i = 0; i < this.slots.length; ++i)
		{
			if (this.slots[i] != null)
			{
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte) i);
				this.slots[i].writeToNBT(nbttagcompound1);
				nbttaglist.appendTag(nbttagcompound1);
			}
		}
		nbt.setTag("Items", nbttaglist);
		if (getInventory().isInvNameLocalized())
		{
			nbt.setString("CustomName", this.costumName);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		NBTTagList nbttaglist = nbt.getTagList("Items");
		this.slots = new ItemStack[getInventory().getSizeInventory()];
		if (nbt.hasKey("CustomName"))
		{
			this.costumName = nbt.getString("CustomName");
		}
		for (int i = 0; i < nbttaglist.tagCount(); ++i)
		{
			NBTTagCompound nbttagcompound1 = (NBTTagCompound) nbttaglist.tagAt(i);
			int j = nbttagcompound1.getByte("Slot") & 255;

			if (j >= 0 && j < this.slots.length)
			{
				this.slots[j] = ItemStack.loadItemStackFromNBT(nbttagcompound1);
			}
		}
	}

	@Override
	public void validate()
	{
		super.validate();
		MinecraftForge.EVENT_BUS.post(new GridTileLoadEvent(this, worldObj, getLocation()));
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		MinecraftForge.EVENT_BUS.post(new GridTileUnloadEvent(this, worldObj, getLocation()));
	}

	@Override
	public WorldCoord getLocation()
	{
		return new WorldCoord(xCoord, yCoord, zCoord);
	}

	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public void setPowerStatus(boolean hasPower)
	{
		powerStatus = hasPower;
	}

	@Override
	public boolean isPowered()
	{
		return powerStatus;
	}

	@Override
	public IGridInterface getGrid()
	{
		return grid;
	}

	@Override
	public void setGrid(IGridInterface gi)
	{
		if (!worldObj.isRemote)
		{
			grid = gi;
			if (getGrid() != null)
				updateFluids(getGrid().getCellArray().getAvailableItems());
		}
	}

	@Override
	public World getWorld()
	{
		return worldObj;
	}

	@Override
	public boolean canConnect(ForgeDirection dir)
	{
		return dir.ordinal() != this.blockMetadata;
	}

	@Override
	public float getPowerDrainPerTick()
	{
		return 5.0F;
	}

	@Override
	public void setNetworkReady(boolean isReady)
	{
		if (!worldObj.isRemote)
		{
			networkReady = isReady;
			if (getGrid() != null)
				updateFluids(getGrid().getCellArray().getAvailableItems());
		}
	}

	@Override
	public boolean isMachineActive()
	{
		return powerStatus && networkReady;
	}
}
