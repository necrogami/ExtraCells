package extracells;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import net.minecraftforge.common.Property.Type;
import net.minecraftforge.oredict.OreDictionary;
import appeng.api.Util;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import extracells.handler.FluidCellHandler;
import extracells.localization.LocalizationHandler;
import extracells.network.AbstractPacket;
import extracells.network.PacketHandler;
import extracells.proxy.CommonProxy;

@Mod(modid = "extracells", name = "Extra Cells", dependencies = "required-after:AppliedEnergistics")
@NetworkMod(channels =
{ AbstractPacket.CHANNEL }, clientSideRequired = true, serverSideRequired = false, packetHandler = PacketHandler.class)
public class Extracells
{

	@Instance("extracells")
	public static Extracells instance;
	public static CreativeTabs ModTab = new CreativeTabs("Extra_Cells")
	{
		public ItemStack getIconItemStack()
		{
			return new ItemStack(StoragePhysical, 1, 4);
		}
	};

	@SidedProxy(clientSide = "extracells.proxy.ClientProxy", serverSide = "extracells.proxy.CommonProxy")
	public static CommonProxy proxy;

	public static Item Cluster;
	public static Item StoragePhysical;
	public static Item StoragePhysicalEncrypted;
	public static Item StoragePhysicalDecrypted;
	public static Item StorageFluid;
	public static Item Casing;
	public static Item FluidDisplay;
	public static int StoragePhysical_ID;
	public static int StoragePhysicalEncrypted_ID;
	public static int StoragePhysicalDecrypted_ID;
	public static int StorageFluid_ID;
	public static int Cluster_ID;
	public static int Casing_ID;
	public static int FluidDisplay_ID;
	public static boolean debug;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		NetworkRegistry.instance().registerGuiHandler(this, proxy);
		instance = this;
		LocalizationHandler.loadLanguages();

		// Config
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		// Items
		int clusterTemp = config.getItem("Cluster_ID", 4141, "ID for the storage clusters (crafting)").getInt();
		int storagePhysicalTemp = config.getItem("Cell_ID", 4140, "ID for the storage cells").getInt();
		int storagePhysicalEncryptedTemp = config.getItem("Cell_Encrypted_ID", 4142, "ID for the encrypted storage cell").getInt();
		int storagePhysicalDecryptedTemp = config.getItem("Cell_Decrypted_ID", 4143, "ID for the decrypted storage cell").getInt();
		int casingTemp = config.getItem("Advanced_Storage_Casing ID", 4144, "ID for the advanced storage casing").getInt();
		int fluidDisplayTemp = config.getItem("Fluid_Display_Item_ID", 4145, "ID item used for displaying fluids in the terminal").getInt();
		int storageFluidTemp = config.getItem("StorageFluid_ID", 4146, "ID for the ME Fluid Storages").getInt();

		// Blocks
		BlockEnum.SOLDERINGSTATION.setID(config.getBlock("SolderingStation_ID", 500, "ID for the soldering station").getInt());
		BlockEnum.MEDROPPER.setID(config.getBlock("MEDropper_ID", 501, "ID for the ME Item Dropper").getInt());
		BlockEnum.MEBATTERY.setID(config.getBlock("MEBattery_ID", 502, "ID for the ME Backup Battery").getInt());
		BlockEnum.BRMEDRIVE.setID(config.getBlock("HardMEDrive_ID", 503, "ID for the Blast Resistant ME Drive").getInt());
		BlockEnum.FLUIDIMPORT.setID(config.getBlock("BusFluidImport_ID", 504, "ID for the Fluid Import Bus").getInt());
		BlockEnum.FLUIDEXPORT.setID(config.getBlock("BusFluidExport_ID", 505, "ID for the Fluid Export Bus").getInt());
		BlockEnum.FLUIDSTORAGE.setID(config.getBlock("BusFluidStorage_ID", 506, "ID for the Fluid Storage Bus").getInt());
		BlockEnum.FLUIDTERMINAL.setID(config.getBlock("TerminalFluid_ID", 507, "ID for the Fluid Storage Terminal").getInt());
		BlockEnum.FLUIDTRANSITION.setID(config.getBlock("FluidTransitionPlane_ID", 508, "ID for the Fluid Transition Plance").getInt());
		BlockEnum.CERTUSTANK.setID(config.getBlock("CertusTank_ID", 509, "ID for the ME Certus Tank").getInt());
		BlockEnum.CHROMIA.setID(config.getBlock("Walrus_ID", 510, "ID for the Walrus").getInt());
		debug = config.get("Dev Options", "showFluidsInMETerminal", false, "Dont't activate if you dont want to debug stuff ;)").getBoolean(false);

		String[] spatialTEs = config.get("Utility", "registerSpatialTileEntity", new String[]
		{ "" }, "Register all TileEntities you want to be movable with the Spatial IO port. use the full packet+class path examplemod.tile.superTileEntity THIS IS EXPERIMENTAL!").getStringList();

		config.save();

		Cluster_ID = clusterTemp;
		StoragePhysical_ID = storagePhysicalTemp;
		StoragePhysicalEncrypted_ID = storagePhysicalEncryptedTemp;
		StoragePhysicalDecrypted_ID = storagePhysicalDecryptedTemp;
		StorageFluid_ID = storageFluidTemp;
		Casing_ID = casingTemp;
		FluidDisplay_ID = fluidDisplayTemp;

		for (String classname : spatialTEs)
		{
			try
			{
				Class<? extends TileEntity> currentClass = (Class<? extends TileEntity>) Class.forName(classname);
				Util.getAppEngApi().getMovableRegistry().whiteListTileEntity(currentClass);
			} catch (Throwable e)
			{
				System.out.println("Tried to register non-existant TileEntity to the SpatialIOPort! " + classname);
			}
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		proxy.RegisterItems();
		proxy.RegisterBlocks();
		proxy.RegisterRenderers();
		proxy.RegisterTileEntities();
		proxy.addRecipes();
		if (!debug)
			Util.addBasicBlackList(extracells.Extracells.FluidDisplay.itemID, OreDictionary.WILDCARD_VALUE);
		Util.getCellRegistry().addCellHandler(new FluidCellHandler());
		LanguageRegistry.instance().addStringLocalization("itemGroup.Extra_Cells", "en_US", "Extra Cells");
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{

	}
}