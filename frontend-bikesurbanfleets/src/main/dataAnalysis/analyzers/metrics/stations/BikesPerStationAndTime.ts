import { Reservation, Station } from "../../../systemDataTypes/Entities";
import { TimeEntry, Event } from "../../../systemDataTypes/SystemInternalData";
import { Observer } from "../../ObserverPattern";

export interface BikesPerTime {
  time: number;
   availableBikes: number;
}

export class StationBikesPerTimeList {
    private bikesPerTimeList: Array<BikesPerTime>;
    
  public constructor(bikes: number) {
    this.bikesPerTimeList = new Array();
    let value: BikesPerTime = {time: 0, availableBikes: bikes};
    this.bikesPerTimeList.push(value);
  }
  
  public addBike(time: number): void {
    let lastPos: number = this.bikesPerTimeList.length-1;
    let bikes: number = this.bikesPerTimeList[lastPos].availableBikes;
    this.bikesPerTimeList.push({time: time, availableBikes: ++bikes});
  }
  
  public substractBike(time: number): void {
    let lastPos: number = this.bikesPerTimeList.length-1;
    let bikes: number = this.bikesPerTimeList[lastPos].availableBikes;
    let value: BikesPerTime = {time: time, availableBikes: --bikes};
    this.bikesPerTimeList.push(value);
  }
    
    public getList(): Array<BikesPerTime> {
        return this.bikesPerTimeList;
    }
}

export class BikesPerStationAndTime implements Observer {
  private stations: Map<number, StationBikesPerTimeList>;
  private reservations: Array<Reservation>;

    public constructor() {
        this.stations = new Map();
    }
    
    public setReservations(reservations: Array<Reservation>): void {
        this.reservations = reservations;
    }
  
    public async init(systemStations: Array<Station>) {
        for(let station of systemStations) {
            let value: StationBikesPerTimeList = new StationBikesPerTimeList(this.obtainInitAvailableBikesOf(station));
            this.stations.set(station.id, value);
        }
    }
  
  public update(timeEntry: TimeEntry): void {
    let instant: number = timeEntry.time;
    let events: Array<Event> = timeEntry.events;
      
    let lastPos: number;
    let reservationId: number;
    let reservation: Reservation | undefined;
    let station: Station;
      
    for(let event of events) {
      let eventStations: Array<Station> = event.changes.stations;
        
       switch(event.name) {
        case "EventUserAppears": {
          if (eventStations !== undefined) {
            // If there are several bike reservations, only the last can be active
            station = eventStations[eventStations.length-1];
            lastPos = station.reservations.new.id.length-1;
            reservationId = station.reservations.new.id[lastPos];
            reservation = this.getReservation(reservationId);
                
            if (reservation !== undefined && reservation.state === "ACTIVE") {  // and, of course, reservationtype = BIKE
                // Decreasing available bikes at the time the UserAppears event's happened
                let stationId: number = reservation.station.id;
                let bikesList: StationBikesPerTimeList | undefined = this.stations.get(stationId);
                if (bikesList !== undefined) {
                    bikesList.substractBike(instant);
                }
            }
          }
          break;
        }
              
        // "EventUserArrivesAtStationToRentBikeWithReservation" mustn't be analized because the bike was already unavailable when the reservation was made in user appearance 
          
        case "EventUserArrivesAtStationToRentBikeWithoutReservation": {
            if (eventStations.length > 0) {
                station = eventStations[0];
                    
                // If bike ids have been registered, a change has occurred (there's 1 bike less)
                if (station.bikes !== undefined) {  // rental + mayvbe, slot reservations
                    let bikesList: StationBikesPerTimeList | undefined = this.stations.get(station.id);
                    if (bikesList !== undefined) {
                        bikesList.substractBike(instant);
                    }
                }
                
                else { // (station.reservations !== undefined)
                    station = eventStations[eventStations.length-1];
                    
                    lastPos = station.reservations.new.id.length-1;
                    reservationId = station.reservations.new.id[lastPos];
                    reservation = this.getReservation(reservationId);
                    if (reservation !== undefined && reservation.state === "ACTIVE") {  // and, of course, reservation.type === "BIKE"  
                        // Decreasing available bikes at the time the UserAppears event''s happened
                        let stationId: number = reservation.station.id;  
                        let bikesList: StationBikesPerTimeList | undefined = this.stations.get(stationId);
                        if (bikesList !== undefined) {
                            bikesList.substractBike(instant);
                        }
                    }
                }
            }
            break;
        }
          
        case "EventBikeReservationTimeout": {
            let historyReservations: Array<Reservation> = event.changes.reservations;
            reservation = this.getReservation(historyReservations[0].id);
            if (reservation !== undefined) {
                // As reservation state is expired, the iinvolved station has one more available bike
                let stationId: number = reservation.station.id;
                let bikesList: StationBikesPerTimeList | undefined = this.stations.get(stationId);
                if (bikesList !== undefined) {
                    bikesList.addBike(instant);
                }
            }
            break;
        }
          
        case "EventUserArrivesAtStationToReturnBikeWithReservation": {
            // only a station can have registered changes (the one the user's reached)
            station = eventStations[0];
            let bikesList: StationBikesPerTimeList | undefined = this.stations.get(station.id);
            if (bikesList !== undefined) {
                bikesList.addBike(instant);
               }
            break;
        }
          
        case "EventUserArrivesAtStationToReturnBikeWithoutReservation": {
            if (eventStations.length > 0) {
                station = eventStations[0];
                if (station.bikes !== undefined) {
                    let bikesList: StationBikesPerTimeList | undefined = this.stations.get(station.id);
                    if (bikesList !== undefined) {
                        bikesList.addBike(instant);
                    }
                }
            }
            break;
        }
      }
    }
  }
    
  private obtainInitAvailableBikesOf(station: Station): number {
    let counter = 0;
    station.bikes.id.forEach( (bikeId: number) => {
        if (bikeId !== null) { 
            counter++;
        }
    });
    return counter;
  }
  
   private getReservation(id: number): Reservation | undefined {
    for(let reservation of this.reservations) {
      if (reservation.id === id) {
        return reservation;
      }
    }
    return undefined;
  }
    
    public getStations(): Map<number, StationBikesPerTimeList> {
        return this.stations;
    } 

}
