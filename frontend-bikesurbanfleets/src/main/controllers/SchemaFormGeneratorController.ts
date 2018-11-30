import * as fs from 'fs-extra';
import { app } from 'electron';
import * as paths from 'path';
import {EntryPointDataType, SchemaConfig} from "../../shared/ConfigurationInterfaces";
import {IpcUtil, Channel} from "../util";
import SchemaParser from "../configuration/SchemaParser";

export class SchemaFormGeneratorController{

    private static globalConfigurationSchema: any = fs.readJsonSync(paths.join(app.getAppPath(), 'schema/global-config.json'));
    private static stationConfigurationSchema: any = fs.readJsonSync(paths.join(app.getAppPath(), 'schema/stations-config.json'));
    private static entryPointsConfSchema: any = fs.readJsonSync(paths.join(app.getAppPath(), 'schema/entrypoints-config.json'));


    static create() {
        return new SchemaFormGeneratorController();
    }

    static enableIpc(): void {
        IpcUtil.openChannel('form-schema-init', async () => {

            const channels = [
                new Channel('form-schema-entry-user-type', async () => this.schemaFormEntryPointAndUserTypes()),
                new Channel('form-schema-entry-point-by-type', async (dataTypes: EntryPointDataType) => this.schemaFormEntryPointByTypes(dataTypes)),
                new Channel('form-schema-station', async () => this.schemaFormStation()),
                new Channel('form-schema-global', async () => this.schemaFormGlobal())
            ];

            channels.forEach((channel) => IpcUtil.openChannel(channel.name, channel.callback));

            IpcUtil.openChannel('form-schema-close', async () => {
                IpcUtil.closeChannels('form-schema-close', ...channels.map((channel) => channel.name));
                this.enableIpc();
            });

            IpcUtil.closeChannel('form-schema-init');
        });
    }

    public static async schemaFormEntryPointAndUserTypes(): Promise<SchemaConfig> {
        let schema: SchemaConfig;
        let entryPointTypes: Array<string> = await SchemaParser.readEntryPointTypes(this.entryPointsConfSchema);
        let userTypes: Array<string> = await SchemaParser.readUserTypes(this.entryPointsConfSchema);
        schema = {
            type: "object",
            properties: {
                entryPointType: {
                    type: "string",
                    enum: entryPointTypes
                },
                userType: {
                    type: "string",
                    enum: userTypes
                }
            },
            required: ["entryPointType", "userType"]
        };
        return schema;
    }

    public static async schemaFormEntryPointByTypes(dataTypes: EntryPointDataType): Promise<string | undefined> {
        let entryPointSchema = await SchemaParser.getEntryPointSchema(this.entryPointsConfSchema, dataTypes.entryPointType, dataTypes.userType);

        if(entryPointSchema !== undefined) {
            return JSON.stringify(entryPointSchema);
        }
        else {
            throw new Error("Entry Point type or user Type is not valid");
        }
    }

    public static async schemaFormStation(): Promise<string | undefined> {
        let stationSchema = await SchemaParser.getStationSchema(this.stationConfigurationSchema);
        if(stationSchema !== undefined) {
            return JSON.stringify(stationSchema);
        }
        else {
            throw new Error("Station is not valid or is not defined in schemas");
        }
    }

    public static async schemaFormGlobal(): Promise<string | undefined> {
        let globalSchema = await SchemaParser.getGlobalSchema(this.globalConfigurationSchema);
        if(globalSchema !== undefined) {
            return JSON.stringify(globalSchema);
        }
        else {
            throw new Error("Global configuration schema error");
        }
    }
}


